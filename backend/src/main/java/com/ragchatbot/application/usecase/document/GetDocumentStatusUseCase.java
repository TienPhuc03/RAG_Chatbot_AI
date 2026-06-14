package com.ragchatbot.application.usecase.document;

import com.ragchatbot.application.dto.document.DocumentStatusResponse;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

/*
 *Use case lấy trạng thái xử lý của tài liệu.
 *Logic: indexedAt != null -> INDEXED, null -> PROCESSING.
 */
@Service
public class GetDocumentStatusUseCase {

    private final DocumentRepository documentRepository;

    public GetDocumentStatusUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public DocumentStatusResponse execute(UUID id) {
        //tìm document, ném lỗi nếu không tồn tại
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        //indexedAt được set sau khi embed xong -> dùng để suy ra trạng thái
        DocumentStatus status = doc.getIndexedAt() != null
                ? DocumentStatus.INDEXED
                : DocumentStatus.PROCESSING;

        return new DocumentStatusResponse(doc.getId(), status, doc.getIndexedAt());
    }
}