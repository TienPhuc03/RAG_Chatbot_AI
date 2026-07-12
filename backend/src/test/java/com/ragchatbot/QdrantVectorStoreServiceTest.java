package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.config.QdrantProperties;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import com.ragchatbot.infrastructure.vectorstore.QdrantVectorStoreService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.assertj.core.data.Offset;

class QdrantVectorStoreServiceTest {

    private QdrantClient qdrantClient;
    private QdrantProperties properties;
    private EmbeddingProperties embeddingProperties;
    private DocumentRepository documentRepository;
    private QdrantVectorStoreService service;

    @BeforeEach
    void setUp() {
        qdrantClient = mock(QdrantClient.class);
        properties = new QdrantProperties();
        properties.setCollectionName("rag_chunks");
        properties.setRequestTimeout(java.time.Duration.ofSeconds(5));
        embeddingProperties = new EmbeddingProperties();
        embeddingProperties.getVectorDimensions().put(EmbeddingModel.GEMINI_EMBEDDING_001, 3);
        embeddingProperties.getVectorDimensions().put(EmbeddingModel.BGE_M3, 3);
        documentRepository = mock(DocumentRepository.class);

        when(qdrantClient.collectionExistsAsync(anyString(), any())).thenReturn(Futures.immediateFuture(true));
        when(qdrantClient.createCollectionAsync(anyString(), org.mockito.ArgumentMatchers.<VectorParams>any(), any()))
                .thenReturn(Futures.immediateFuture(null));
        when(qdrantClient.upsertAsync(anyString(), any(List.class), any())).thenReturn(Futures.immediateFuture(null));
        when(qdrantClient.deleteAsync(anyString(), any(io.qdrant.client.grpc.Points.Filter.class), any()))
                .thenReturn(Futures.immediateFuture(null));
        when(qdrantClient.searchAsync(any(SearchPoints.class), any())).thenReturn(Futures.immediateFuture(List.of()));

        service = new QdrantVectorStoreService(qdrantClient, properties, embeddingProperties, documentRepository);
    }

    @Test
    void upsertBuildsPointsWithExpectedPayload() {
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        document.setCourseCode("JAVA101");
        document.setChapterCode("CH1");
        document.setSourceFileName("java101.pdf");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        ChunkDraft chunk = new ChunkDraft(0, "Noi dung", 2, 3);
        service.upsert(
                documentId,
                EmbeddingModel.BGE_M3,
                ChunkingStrategy.SEMANTIC,
                List.of(chunk),
                List.of(List.of(0.1f, 0.2f, 0.3f))
        );

        ArgumentCaptor<List<PointStruct>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(qdrantClient).upsertAsync(anyString(), pointsCaptor.capture(), any());

        PointStruct point = pointsCaptor.getValue().getFirst();
        assertThat(point.getPayloadMap()).containsKeys(
                "chunk_id",
                "document_id",
                "chunk_index",
                "content",
                "page_number",
                "token_count",
                "course_code",
                "chapter_code",
                "session_id",
                "source_file_name",
                "embedding_model",
                "chunking_strategy"
        );
        assertThat(point.getPayloadMap().get("document_id").getStringValue()).isEqualTo(documentId.toString());
        assertThat(point.getPayloadMap().get("course_code").getStringValue()).isEqualTo("JAVA101");
        assertThat(point.getPayloadMap().get("chapter_code").getStringValue()).isEqualTo("CH1");
        assertThat(point.getPayloadMap().get("source_file_name").getStringValue()).isEqualTo("java101.pdf");
        assertThat(point.getPayloadMap().get("embedding_model").getStringValue()).isEqualTo("BGE_M3");
        assertThat(point.getPayloadMap().get("chunking_strategy").getStringValue()).isEqualTo("SEMANTIC");
        assertThat(point.getVectors().getVector().getDataList()).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void upsertRejectsMismatchedEmbeddingDimension() {
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.upsert(
                documentId,
                EmbeddingModel.GEMINI_EMBEDDING_001,
                ChunkingStrategy.SEMANTIC,
                List.of(new ChunkDraft(0, "Noi dung", 1, 2)),
                List.of(List.of(0.1f, 0.2f))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimension 3");

        verify(qdrantClient, never()).upsertAsync(anyString(), any(List.class), any());
    }

        @Test
        void searchMapsScoredPointToRetrievedContext() throws Exception {
        UUID expectedChunkId = UUID.randomUUID();
        UUID expectedDocumentId = UUID.randomUUID();

        ScoredPoint scoredPoint = ScoredPoint.newBuilder()
                .setScore(0.91f)
                .putPayload(
                        "chunk_id",
                        io.qdrant.client.ValueFactory.value(expectedChunkId.toString())
                )
                .putPayload(
                        "document_id",
                        io.qdrant.client.ValueFactory.value(expectedDocumentId.toString())
                )
                .putPayload(
                        "content",
                        io.qdrant.client.ValueFactory.value("Cau tra loi")
                )
                .putPayload(
                        "course_code",
                        io.qdrant.client.ValueFactory.value("JAVA101")
                )
                .putPayload(
                        "chapter_code",
                        io.qdrant.client.ValueFactory.value("CH1")
                )
                .putPayload(
                        "source_file_name",
                        io.qdrant.client.ValueFactory.value("java101.pdf")
                )
                .putPayload(
                        "page_number",
                        io.qdrant.client.ValueFactory.value(7L)
                )
                .build();

        when(qdrantClient.searchAsync(any(SearchPoints.class), any()))
                .thenReturn(Futures.immediateFuture(List.of(scoredPoint)));

        List<RetrievedContext> contexts = service.search(
                EmbeddingModel.GEMINI_EMBEDDING_001,
                List.of(0.1f, 0.2f, 0.3f),
                5,
                ChunkingStrategy.SEMANTIC,
                "JAVA101",
                "CH1",
                null
        );

        assertThat(contexts).hasSize(1);

        RetrievedContext context = contexts.getFirst();

        // Hai assertion quan trọng nhất.
        assertThat(context.chunkId()).isEqualTo(expectedChunkId);
        assertThat(context.documentId()).isEqualTo(expectedDocumentId);

        assertThat(context.content()).isEqualTo("Cau tra loi");
        assertThat(context.score())
                .isCloseTo(0.91d, Offset.offset(0.00001d));
        assertThat(context.courseCode()).isEqualTo("JAVA101");
        assertThat(context.chapterCode()).isEqualTo("CH1");
        assertThat(context.sourceFileName()).isEqualTo("java101.pdf");
        assertThat(context.pageNumber()).isEqualTo(7);
        assertThat(context.pageStart()).isEqualTo(7);
        assertThat(context.pageEnd()).isEqualTo(7);
        }
        
    @Test
    void deleteByDocumentIdBuildsDeleteFilter() {
        UUID documentId = UUID.randomUUID();

        service.deleteByDocumentId(documentId, EmbeddingModel.GEMINI_EMBEDDING_001);

        verify(qdrantClient).deleteAsync(anyString(), any(io.qdrant.client.grpc.Points.Filter.class), any());
    }

    @Test
    void initializeCollectionCreatesMissingCollection() throws Exception {
        when(qdrantClient.collectionExistsAsync(anyString(), any())).thenReturn(Futures.immediateFuture(false));
        QdrantVectorStoreService initializingService = new QdrantVectorStoreService(
                qdrantClient,
                properties,
                embeddingProperties,
                documentRepository
        );

        Method initialize = QdrantVectorStoreService.class.getDeclaredMethod("initializeCollection");
        initialize.setAccessible(true);
        initialize.invoke(initializingService);

        verify(qdrantClient, times(embeddingProperties.getVectorDimensions().size())).createCollectionAsync(
                anyString(),
                org.mockito.ArgumentMatchers.<VectorParams>any(),
                any()
        );
    }
}
