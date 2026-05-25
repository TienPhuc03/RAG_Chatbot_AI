package com.ragchatbot.domain.port;

import java.util.UUID;

public record RetrievedContext(
        UUID chunkId,
        UUID documentId,
        String content,
        Double score,
        String courseCode,
        String chapterCode
) {
}
