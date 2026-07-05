package com.ragchatbot.domain.port;

import java.util.UUID;

public record RetrievedContext(
        UUID chunkId,
        UUID documentId,
        String content,
        Double score,
        String courseCode,
        String chapterCode,
        String sourceFileName,
        Integer pageNumber
) {

    public CitationReference toCitationReference() {
        return new CitationReference(
                documentId,
                chunkId,
                sourceFileName,
                pageNumber,
                courseCode,
                chapterCode,
                score
        );
    }
}
