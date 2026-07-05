package com.ragchatbot.application.dto.chat;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String sessionId,
        String answer,
        boolean groundedInDocuments,
        List<ChatCitationDto> citations
) {
}
