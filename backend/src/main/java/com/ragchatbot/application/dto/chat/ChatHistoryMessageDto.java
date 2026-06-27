package com.ragchatbot.application.dto.chat;

import com.ragchatbot.domain.enums.MessageRole;
import java.time.Instant;
import java.util.UUID;

public record ChatHistoryMessageDto(
        UUID messageId,
        MessageRole role,
        String content,
        Instant createdAt,
        String citationPayload
) {
}
