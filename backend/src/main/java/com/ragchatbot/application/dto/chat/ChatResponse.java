package com.ragchatbot.application.dto.chat;

/**
 * Response trả về từ chatbot.
 */
public record ChatResponse(
        String answer,
        boolean groundedInDocuments
) {
}