package com.ragchatbot.application.dto.chat;

import com.ragchatbot.domain.enums.DocumentStatus;
import java.util.UUID;

public record ChatAttachmentUploadResponse(
        String sessionId,
        UUID documentId,
        String fileName,
        DocumentStatus status,
        String failureReason
) {
}
