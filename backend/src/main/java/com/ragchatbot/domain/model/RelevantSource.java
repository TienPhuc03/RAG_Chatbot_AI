package com.ragchatbot.domain.model;

public record RelevantSource(
    String documentId,
    String sourceFileName,
    Integer pageStart,
    Integer pageEnd,
    String section
) {}