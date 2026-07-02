package com.ragchatbot.application.dto.chat;

import com.ragchatbot.domain.enums.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

public record ChatAttachmentItemResponse(
        UUID documentId,
        String fileName,
        DocumentStatus status,
        String failureReason,
        Instant indexedAt
) {
}
