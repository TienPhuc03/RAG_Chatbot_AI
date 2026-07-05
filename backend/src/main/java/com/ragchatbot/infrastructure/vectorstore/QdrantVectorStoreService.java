package com.ragchatbot.infrastructure.vectorstore;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.nullValue;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

import com.google.common.util.concurrent.ListenableFuture;
import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.config.QdrantProperties;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QdrantVectorStoreService implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreService.class);

    private static final String PAYLOAD_CHUNK_ID = "chunk_id";
    private static final String PAYLOAD_DOCUMENT_ID = "document_id";
    private static final String PAYLOAD_CHUNK_INDEX = "chunk_index";
    private static final String PAYLOAD_CONTENT = "content";
    private static final String PAYLOAD_PAGE_NUMBER = "page_number";
    private static final String PAYLOAD_TOKEN_COUNT = "token_count";
    private static final String PAYLOAD_COURSE_CODE = "course_code";
    private static final String PAYLOAD_CHAPTER_CODE = "chapter_code";
    private static final String PAYLOAD_SESSION_ID = "session_id";
    private static final String PAYLOAD_EMBEDDING_MODEL = "embedding_model";
    private static final String PAYLOAD_CHUNKING_STRATEGY = "chunking_strategy";
    private static final String PAYLOAD_SOURCE_FILE_NAME = "source_file_name";

    private final QdrantClient qdrantClient;
    private final QdrantProperties properties;
    private final EmbeddingProperties embeddingProperties;
    private final DocumentRepository documentRepository;

    public QdrantVectorStoreService(
            QdrantClient qdrantClient,
            QdrantProperties properties,
            EmbeddingProperties embeddingProperties,
            DocumentRepository documentRepository
    ) {
        this.qdrantClient = qdrantClient;
        this.properties = properties;
        this.embeddingProperties = embeddingProperties;
        this.documentRepository = documentRepository;
    }

    @PostConstruct
    void initializeCollection() {
        try {
            for (EmbeddingModel model : EnumSet.copyOf(embeddingProperties.getVectorDimensions().keySet())) {
                ensureCollection(model);
            }
        } catch (Exception ex) {
            log.warn("Skipping Qdrant collection bootstrap because the service is unavailable", ex);
        }
    }

    @Override
    public void upsert(
            UUID documentId,
            EmbeddingModel embeddingModel,
            ChunkingStrategy chunkingStrategy,
            List<ChunkDraft> chunks,
            List<List<Float>> embeddings
    ) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID must not be null");
        }
        if (embeddingModel == null) {
            throw new IllegalArgumentException("Embedding model must not be null");
        }
        if (chunkingStrategy == null) {
            throw new IllegalArgumentException("Chunking strategy must not be null");
        }
        if (chunks == null || embeddings == null || chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Chunks and embeddings must be non-null and have the same size");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        String collectionName = collectionNameFor(embeddingModel);
        ensureCollection(embeddingModel);

        List<PointStruct> points = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            ChunkDraft chunk = chunks.get(i);
            List<Float> embedding = embeddings.get(i);
            validateEmbedding(embeddingModel, embedding, i);

            UUID pointId = pointId(documentId, chunk.chunkIndex());
            points.add(PointStruct.newBuilder()
                    .setId(id(pointId))
                    .setVectors(vectors(embedding))
                    .putAllPayload(payloadFor(pointId, document, chunk, embeddingModel, chunkingStrategy))
                    .build());
        }

        if (!points.isEmpty()) {
            await(qdrantClient.upsertAsync(
                    collectionName,
                    points,
                    properties.getRequestTimeout()
            ));
        }
    }

    @Override
    public List<RetrievedContext> search(
            EmbeddingModel embeddingModel,
            List<Float> queryEmbedding,
            int topK,
            ChunkingStrategy chunkingStrategy,
            String courseCode,
            String chapterCode,
            String conversationSessionId
    ) {
        if (topK <= 0) {
            return List.of();
        }
        if (embeddingModel == null) {
            throw new IllegalArgumentException("Embedding model must not be null");
        }
        ensureCollection(embeddingModel);
        validateEmbedding(embeddingModel, queryEmbedding, -1);

        SearchPoints.Builder searchBuilder = SearchPoints.newBuilder()
                .setCollectionName(collectionNameFor(embeddingModel))
                .addAllVector(queryEmbedding)
                .setLimit(topK)
                .setWithPayload(enable(true));

        Filter filter = filterFor(chunkingStrategy, courseCode, chapterCode, conversationSessionId);
        if (filter != null) {
            searchBuilder.setFilter(filter);
        }

        return await(qdrantClient.searchAsync(searchBuilder.build(), properties.getRequestTimeout()))
                .stream()
                .map(this::toRetrievedContext)
                .toList();
    }

    @Override
    public void deleteByDocumentId(UUID documentId, EmbeddingModel embeddingModel) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID must not be null");
        }
        if (embeddingModel == null) {
            throw new IllegalArgumentException("Embedding model must not be null");
        }
        ensureCollection(embeddingModel);

        Filter filter = Filter.newBuilder()
                .addMust(matchKeyword(PAYLOAD_DOCUMENT_ID, documentId.toString()))
                .build();

        await(qdrantClient.deleteAsync(
                collectionNameFor(embeddingModel),
                filter,
                properties.getRequestTimeout()
        ));
    }

    @PreDestroy
    void close() {
        qdrantClient.close();
    }

    private Map<String, Value> payloadFor(
            UUID pointId,
            Document document,
            ChunkDraft chunk,
            EmbeddingModel embeddingModel,
            ChunkingStrategy chunkingStrategy
    ) {
        Map<String, Value> payload = new LinkedHashMap<>();
        payload.put(PAYLOAD_CHUNK_ID, value(pointId.toString()));
        payload.put(PAYLOAD_DOCUMENT_ID, value(document.getId().toString()));
        payload.put(PAYLOAD_CHUNK_INDEX, value((long) chunk.chunkIndex()));
        payload.put(PAYLOAD_CONTENT, value(chunk.content()));
        payload.put(PAYLOAD_PAGE_NUMBER, nullableLong(chunk.pageNumber()));
        payload.put(PAYLOAD_TOKEN_COUNT, nullableLong(chunk.tokenCount()));
        payload.put(PAYLOAD_COURSE_CODE, nullableString(document.getCourseCode()));
        payload.put(PAYLOAD_CHAPTER_CODE, nullableString(document.getChapterCode()));
        payload.put(PAYLOAD_SESSION_ID, nullableString(document.getConversationSessionId()));
        payload.put(PAYLOAD_SOURCE_FILE_NAME, nullableString(document.getSourceFileName()));
        payload.put(PAYLOAD_EMBEDDING_MODEL, value(embeddingModel.name()));
        payload.put(PAYLOAD_CHUNKING_STRATEGY, value(chunkingStrategy.name()));
        return payload;
    }

    private Filter filterFor(
            ChunkingStrategy chunkingStrategy,
            String courseCode,
            String chapterCode,
            String conversationSessionId
    ) {
        Filter.Builder filterBuilder = Filter.newBuilder();
        if (chunkingStrategy != null) {
            filterBuilder.addMust(matchKeyword(PAYLOAD_CHUNKING_STRATEGY, chunkingStrategy.name()));
        }
        if (hasText(conversationSessionId)) {
            filterBuilder.addMust(matchKeyword(PAYLOAD_SESSION_ID, conversationSessionId));
        }
        if (hasText(courseCode)) {
            filterBuilder.addMust(matchKeyword(PAYLOAD_COURSE_CODE, courseCode));
        }
        if (hasText(chapterCode)) {
            filterBuilder.addMust(matchKeyword(PAYLOAD_CHAPTER_CODE, chapterCode));
        }
        return filterBuilder.getMustCount() == 0 ? null : filterBuilder.build();
    }

    private RetrievedContext toRetrievedContext(ScoredPoint point) {
        Map<String, Value> payload = point.getPayloadMap();
        UUID chunkId = uuidValue(payload, PAYLOAD_CHUNK_ID);
        UUID documentId = uuidValue(payload, PAYLOAD_DOCUMENT_ID);
        if (chunkId == null && point.hasId() && point.getId().hasUuid()) {
            chunkId = UUID.fromString(point.getId().getUuid());
        }

        return new RetrievedContext(
                chunkId,
                documentId,
                stringValue(payload, PAYLOAD_CONTENT),
                (double) point.getScore(),
                stringValue(payload, PAYLOAD_COURSE_CODE),
                stringValue(payload, PAYLOAD_CHAPTER_CODE),
                stringValue(payload, PAYLOAD_SOURCE_FILE_NAME),
                integerValue(payload, PAYLOAD_PAGE_NUMBER)
        );
    }

    private UUID pointId(UUID documentId, int chunkIndex) {
        return UUID.nameUUIDFromBytes((documentId + ":" + chunkIndex).getBytes(StandardCharsets.UTF_8));
    }

    private void validateEmbedding(EmbeddingModel embeddingModel, List<Float> embedding, int index) {
        int expectedDimension = vectorSizeFor(embeddingModel);
        if (embedding == null || embedding.size() != expectedDimension) {
            String subject = index < 0 ? "Query embedding" : "Embedding at index " + index;
            throw new IllegalArgumentException(
                    subject + " must have dimension " + expectedDimension
            );
        }
    }

    private void ensureCollection(EmbeddingModel embeddingModel) {
        String collectionName = collectionNameFor(embeddingModel);
        boolean exists = await(qdrantClient.collectionExistsAsync(
                collectionName,
                properties.getRequestTimeout()
        ));

        if (!exists) {
            VectorParams vectorParams = VectorParams.newBuilder()
                    .setSize(vectorSizeFor(embeddingModel))
                    .setDistance(Distance.Cosine)
                    .build();

            await(qdrantClient.createCollectionAsync(
                    collectionName,
                    vectorParams,
                    properties.getRequestTimeout()
            ));
        }
    }

    private String collectionNameFor(EmbeddingModel embeddingModel) {
        return properties.getCollectionName() + "_" + embeddingModel.name().toLowerCase();
    }

    private int vectorSizeFor(EmbeddingModel embeddingModel) {
        Integer vectorSize = embeddingProperties.getVectorDimensions().get(embeddingModel);
        if (vectorSize == null || vectorSize <= 0) {
            throw new IllegalArgumentException("Missing vector dimension for embedding model " + embeddingModel);
        }
        return vectorSize;
    }

    private Value nullableString(String string) {
        return string == null ? nullValue() : value(string);
    }

    private Value nullableLong(Integer number) {
        return number == null ? nullValue() : value(number.longValue());
    }

    private UUID uuidValue(Map<String, Value> payload, String key) {
        String string = stringValue(payload, key);
        return string == null ? null : UUID.fromString(string);
    }

    private String stringValue(Map<String, Value> payload, String key) {
        Value value = payload.get(key);
        return value != null && value.hasStringValue() ? value.getStringValue() : null;
    }

    private Integer integerValue(Map<String, Value> payload, String key) {
        Value value = payload.get(key);
        return value != null && value.hasIntegerValue() ? Math.toIntExact(value.getIntegerValue()) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> T await(ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Qdrant", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Qdrant request failed", ex.getCause());
        }
    }
}
