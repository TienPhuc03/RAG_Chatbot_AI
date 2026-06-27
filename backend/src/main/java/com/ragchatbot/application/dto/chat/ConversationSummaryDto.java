package com.ragchatbot.application.dto.chat;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryDto(
        UUID conversationId,
        String sessionId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
