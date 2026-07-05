package com.ragchatbot.application.usecase.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.application.dto.document.DocumentUploadResponse;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class UploadDocumentUseCaseTest {

    @Test
    void executeCreatesPendingDocumentAndDispatchesWorker() {
        DocumentIndexingWorker worker = mock(DocumentIndexingWorker.class);
        DocumentRepository repository = mock(DocumentRepository.class);
        EmbeddingProperties embeddingProperties = new EmbeddingProperties();
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UploadDocumentUseCase useCase = new UploadDocumentUseCase(worker, repository, embeddingProperties);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lecture-1.pdf",
                "application/pdf",
                "hello world".getBytes(StandardCharsets.UTF_8)
        );

        DocumentUploadResponse response = useCase.execute(
                file,
                "JAVA101",
                "Java Basics",
                "CH1",
                "Intro",
                ChunkingStrategy.SEMANTIC,
                EmbeddingModel.BGE_M3
        );

        assertThat(response.status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(response.courseCode()).isEqualTo("JAVA101");
        assertThat(response.embeddingModel()).isEqualTo(EmbeddingModel.BGE_M3);
        verify(worker).process(any(DocumentUploadJob.class));
    }

    @Test
    void executeAllowsSameFileForDifferentResearchRuns() {
        DocumentIndexingWorker worker = mock(DocumentIndexingWorker.class);
        DocumentRepository repository = mock(DocumentRepository.class);
        EmbeddingProperties embeddingProperties = new EmbeddingProperties();
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UploadDocumentUseCase useCase = new UploadDocumentUseCase(worker, repository, embeddingProperties);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lecture-duplicate.pdf",
                "application/pdf",
                "duplicate".getBytes(StandardCharsets.UTF_8)
        );

        DocumentUploadResponse first = useCase.execute(
                file,
                "JAVA101",
                "Java Basics",
                "CH1",
                "Intro",
                ChunkingStrategy.FIXED_SIZE,
                EmbeddingModel.MULTILINGUAL_E5_BASE
        );
        DocumentUploadResponse second = useCase.execute(
                file,
                "JAVA101",
                "Java Basics",
                "CH1",
                "Intro",
                ChunkingStrategy.SEMANTIC,
                null
        );

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(second.embeddingModel()).isEqualTo(EmbeddingModel.GEMINI_EMBEDDING_001);
        verify(worker, times(2)).process(any(DocumentUploadJob.class));
    }
}
