package com.ragchatbot.application.usecase.chat;

import com.ragchatbot.application.dto.chat.ChatAttachmentItemResponse;
import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetChatAttachmentsUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentMaintenanceService documentMaintenanceService;

    public GetChatAttachmentsUseCase(
            DocumentRepository documentRepository,
            DocumentMaintenanceService documentMaintenanceService
    ) {
        this.documentRepository = documentRepository;
        this.documentMaintenanceService = documentMaintenanceService;
    }

    public List<ChatAttachmentItemResponse> execute(String sessionId) {
        documentMaintenanceService.reconcileStaleProcessingDocuments();
        return documentRepository.findByConversationSessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(document -> new ChatAttachmentItemResponse(
                        document.getId(),
                        document.getSourceFileName(),
                        document.getStatus(),
                        document.getFailureReason(),
                        document.getIndexedAt()
                ))
                .toList();
    }
}
