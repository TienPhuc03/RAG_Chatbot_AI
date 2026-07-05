package com.ragchatbot.application.dto.chat;

import java.util.UUID;

public record ChatCitationDto(
        UUID documentId,
        UUID chunkId,
        String sourceFileName,
        Integer pageNumber,
        String courseCode,
        String chapterCode,
        Double score
) {
}
