package com.ragchatbot.application.usecase.document;

import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentMaintenanceService {

    private static final Duration STALE_PROCESSING_TIMEOUT = Duration.ofMinutes(5);
    private static final String STALE_PROCESSING_REASON =
            "Tien trinh index truoc do da bi gian doan. Vui long upload lai tai lieu de index lai.";

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    public DocumentMaintenanceService(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    public void reconcileStaleProcessingDocuments() {
        Instant threshold = Instant.now().minus(STALE_PROCESSING_TIMEOUT);
        List<Document> staleDocuments = documentRepository.findByStatusAndUpdatedAtBefore(
                DocumentStatus.PROCESSING,
                threshold
        );

        if (staleDocuments.isEmpty()) {
            return;
        }

        for (Document document : staleDocuments) {
            document.setStatus(DocumentStatus.FAILED);
            if (document.getFailureReason() == null || document.getFailureReason().isBlank()) {
                document.setFailureReason(STALE_PROCESSING_REASON);
            }
        }

        documentRepository.saveAll(staleDocuments);
    }

    public boolean hasIndexedDocuments(String courseCode, String chapterCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return documentRepository.countByStatusAndConversationSessionIdIsNull(DocumentStatus.INDEXED) > 0;
        }

        if (chapterCode == null || chapterCode.isBlank()) {
            return documentRepository.countByStatusAndCourseCodeAndConversationSessionIdIsNull(
                    DocumentStatus.INDEXED,
                    courseCode.trim()
            ) > 0;
        }

        return documentRepository.countByStatusAndCourseCodeAndChapterCodeAndConversationSessionIdIsNull(
                DocumentStatus.INDEXED,
                courseCode.trim(),
                chapterCode.trim()
        ) > 0;
    }

    public boolean hasIndexedDocumentsForConversation(String conversationSessionId) {
        if (conversationSessionId == null || conversationSessionId.isBlank()) {
            return false;
        }

        return documentRepository.countByStatusAndConversationSessionId(
                DocumentStatus.INDEXED,
                conversationSessionId.trim()
        ) > 0;
    }

    public boolean hasIndexedDocumentsForBenchmark(
            ChunkingStrategy chunkingStrategy,
            EmbeddingModel embeddingModel
    ) {
        if (chunkingStrategy == null || embeddingModel == null) {
            return false;
        }

        return chunkRepository.countIndexedPublicDocumentsForBenchmark(
                chunkingStrategy,
                embeddingModel
        ) > 0;
    }
}
