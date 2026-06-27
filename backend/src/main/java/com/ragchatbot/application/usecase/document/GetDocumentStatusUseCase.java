package com.ragchatbot.application.usecase.document;

import com.ragchatbot.application.dto.document.DocumentStatusResponse;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetDocumentStatusUseCase {

    private final DocumentRepository documentRepository;

    public GetDocumentStatusUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public DocumentStatusResponse execute(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        DocumentStatus status = doc.getStatus() == null ? DocumentStatus.PENDING : doc.getStatus();

        return new DocumentStatusResponse(doc.getId(), status, doc.getIndexedAt());
    }
}
