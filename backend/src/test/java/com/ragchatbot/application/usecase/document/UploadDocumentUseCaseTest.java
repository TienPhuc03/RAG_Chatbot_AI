package com.ragchatbot.application.usecase.document;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockMultipartFile;

import com.ragchatbot.application.dto.document.DocumentUploadResponse;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;

class UploadDocumentUseCaseTest {

    @Test
    void executeCreatesPendingDocumentAndDispatchesWorker() {
        DocumentIndexingWorker worker = mock(DocumentIndexingWorker.class);
        DocumentRepository repository = mock(DocumentRepository.class);
        when(repository.findByChecksum(any())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UploadDocumentUseCase useCase = new UploadDocumentUseCase(worker, repository);
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
                ChunkingStrategy.SEMANTIC
        );

        assertThat(response.status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(response.courseCode()).isEqualTo("JAVA101");
        verify(worker).process(any(DocumentUploadJob.class));
    }

    @Test
    void executeReturnsExistingDocumentForDuplicateChecksum() {
        DocumentIndexingWorker worker = mock(DocumentIndexingWorker.class);
        DocumentRepository repository = mock(DocumentRepository.class);
        Document existing = new Document();
        existing.setId(UUID.randomUUID());
        existing.setTitle("Existing");
        existing.setSourceFileName("existing.pdf");
        existing.setCourseCode("JAVA101");
        existing.setStatus(DocumentStatus.INDEXED);
        when(repository.findByChecksum(any())).thenReturn(Optional.of(existing));

        UploadDocumentUseCase useCase = new UploadDocumentUseCase(worker, repository);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lecture-duplicate.pdf",
                "application/pdf",
                "duplicate".getBytes(StandardCharsets.UTF_8)
        );

        DocumentUploadResponse response = useCase.execute(
                file,
                "JAVA101",
                "Java Basics",
                "",
                "",
                 ChunkingStrategy.SEMANTIC
        );

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.status()).isEqualTo(DocumentStatus.INDEXED);
        verify(worker, never()).process(any());
    }
}
