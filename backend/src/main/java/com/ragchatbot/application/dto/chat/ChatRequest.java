package com.ragchatbot.application.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request gửi câu hỏi tới chatbot.
 */

// backend tự tạo session Id
public record ChatRequest(

        @Size(max = 100, message = "Session ID must be at most 100 characters")
        String sessionId,

        @NotBlank(message = "Question must not be blank")
        @Size(max = 1000, message = "Question must be at most 1000 characters")
        String question,

        String courseCode,

        String chapterCode

) {
}