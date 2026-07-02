package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.ragchatbot.config.QdrantProperties;
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
    private DocumentRepository documentRepository;
    private QdrantVectorStoreService service;

    @BeforeEach
    void setUp() {
        qdrantClient = mock(QdrantClient.class);
        properties = new QdrantProperties();
        properties.setCollectionName("rag_chunks");
        properties.setVectorSize(3);
        properties.setRequestTimeout(java.time.Duration.ofSeconds(5));
        documentRepository = mock(DocumentRepository.class);

        when(qdrantClient.collectionExistsAsync(anyString(), any())).thenReturn(Futures.immediateFuture(true));
        when(qdrantClient.createCollectionAsync(anyString(), org.mockito.ArgumentMatchers.<VectorParams>any(), any()))
                .thenReturn(Futures.immediateFuture(null));
        when(qdrantClient.upsertAsync(anyString(), any(List.class), any())).thenReturn(Futures.immediateFuture(null));
        when(qdrantClient.deleteAsync(anyString(), any(io.qdrant.client.grpc.Points.Filter.class), any()))
                .thenReturn(Futures.immediateFuture(null));
        when(qdrantClient.searchAsync(any(SearchPoints.class), any())).thenReturn(Futures.immediateFuture(List.of()));

        service = new QdrantVectorStoreService(qdrantClient, properties, documentRepository);
    }

    @Test
    void upsertBuildsPointsWithExpectedPayload() {
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        document.setCourseCode("JAVA101");
        document.setChapterCode("CH1");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        ChunkDraft chunk = new ChunkDraft(0, "Noi dung", 2, 3);
        service.upsert(documentId, List.of(chunk), List.of(List.of(0.1f, 0.2f, 0.3f)));

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
                "session_id"
        );
        assertThat(point.getPayloadMap().get("document_id").getStringValue()).isEqualTo(documentId.toString());
        assertThat(point.getPayloadMap().get("course_code").getStringValue()).isEqualTo("JAVA101");
        assertThat(point.getPayloadMap().get("chapter_code").getStringValue()).isEqualTo("CH1");
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
                List.of(new ChunkDraft(0, "Noi dung", 1, 2)),
                List.of(List.of(0.1f, 0.2f))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimension 3");

        verify(qdrantClient, never()).upsertAsync(anyString(), any(List.class), any());
    }

    @Test
    void searchMapsScoredPointToRetrievedContext() throws Exception {
        ScoredPoint scoredPoint = ScoredPoint.newBuilder()
                .setScore(0.91f)
                .putPayload("chunk_id", io.qdrant.client.ValueFactory.value(UUID.randomUUID().toString()))
                .putPayload("document_id", io.qdrant.client.ValueFactory.value(UUID.randomUUID().toString()))
                .putPayload("content", io.qdrant.client.ValueFactory.value("Cau tra loi"))
                .putPayload("course_code", io.qdrant.client.ValueFactory.value("JAVA101"))
                .putPayload("chapter_code", io.qdrant.client.ValueFactory.value("CH1"))
                .build();

        when(qdrantClient.searchAsync(any(SearchPoints.class), any()))
                .thenReturn(Futures.immediateFuture(List.of(scoredPoint)));

        List<RetrievedContext> contexts = service.search(
                List.of(0.1f, 0.2f, 0.3f),
                5,
                "JAVA101",
                "CH1",
                null
        );

        assertThat(contexts).hasSize(1);
        assertThat(contexts.getFirst().content()).isEqualTo("Cau tra loi");
        assertThat(contexts.getFirst().score()).isCloseTo(0.91d, Offset.offset(0.00001d));
        assertThat(contexts.getFirst().courseCode()).isEqualTo("JAVA101");
        assertThat(contexts.getFirst().chapterCode()).isEqualTo("CH1");
    }

    @Test
    void deleteByDocumentIdBuildsDeleteFilter() {
        UUID documentId = UUID.randomUUID();

        service.deleteByDocumentId(documentId);

        verify(qdrantClient).deleteAsync(anyString(), any(io.qdrant.client.grpc.Points.Filter.class), any());
    }

    @Test
    void initializeCollectionCreatesMissingCollection() throws Exception {
        when(qdrantClient.collectionExistsAsync(anyString(), any())).thenReturn(Futures.immediateFuture(false));
        QdrantVectorStoreService initializingService = new QdrantVectorStoreService(qdrantClient, properties, documentRepository);

        Method initialize = QdrantVectorStoreService.class.getDeclaredMethod("initializeCollection");
        initialize.setAccessible(true);
        initialize.invoke(initializingService);

        verify(qdrantClient).createCollectionAsync(
                anyString(),
                org.mockito.ArgumentMatchers.<VectorParams>any(),
                any()
        );
    }
}
