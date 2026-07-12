package com.ragchatbot.domain.port;

import java.util.UUID;

// import jakarta.persistence.criteria.CriteriaBuilder.In;

public record RetrievedContext(
        UUID chunkId,
        UUID documentId,
        String content,
        Double score,
        String courseCode,
        String chapterCode,
        String sourceFileName,
        Integer pageNumber,
        Integer pageStart,
        Integer pageEnd,
        String section
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
