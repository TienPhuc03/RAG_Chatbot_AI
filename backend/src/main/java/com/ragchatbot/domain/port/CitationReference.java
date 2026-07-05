package com.ragchatbot.domain.port;

import java.util.UUID;

public record CitationReference(
        UUID documentId,
        UUID chunkId,
        String sourceFileName,
        Integer pageNumber,
        String courseCode,
        String chapterCode,
        Double score
) {
}
