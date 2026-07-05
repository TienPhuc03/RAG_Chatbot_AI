package com.ragchatbot.application.usecase.document;

import com.ragchatbot.application.dto.document.DocumentStatusResponse;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetDocumentStatusUseCase {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentMaintenanceService documentMaintenanceService;

    public GetDocumentStatusUseCase(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository,
            DocumentMaintenanceService documentMaintenanceService
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentMaintenanceService = documentMaintenanceService;
    }

    public DocumentStatusResponse execute(UUID id) {
        documentMaintenanceService.reconcileStaleProcessingDocuments();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        DocumentStatus status = doc.getStatus() == null ? DocumentStatus.PENDING : doc.getStatus();
        var firstChunk = chunkRepository.findTopByDocumentIdOrderByChunkIndexAsc(doc.getId()).orElse(null);

        return new DocumentStatusResponse(
                doc.getId(),
                status,
                doc.getIndexedAt(),
                doc.getFailureReason(),
                firstChunk == null ? null : firstChunk.getEmbeddingModel()
        );
    }
}
