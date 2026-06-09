package com.ragchatbot.application.dto.chat;

/**
 * Request gửi câu hỏi tới chatbot.
 */
public record ChatRequest(
        String sessionId,
        String question,
        String courseCode,
        String chapterCode
) {
}