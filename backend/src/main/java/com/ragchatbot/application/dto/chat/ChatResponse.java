package com.ragchatbot.application.dto.chat;

/**
 * Response trả về từ chatbot.
 */
public record ChatResponse(
        String conversationId,      //id thật trong db, backend
        String sessionId,           //id public cho frontend
        String answer,
        boolean groundedInDocuments
) {
}