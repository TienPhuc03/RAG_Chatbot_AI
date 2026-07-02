package com.ragchatbot.application.usecase.document;

import java.util.UUID;

import com.ragchatbot.domain.enums.ChunkingStrategy;

public record DocumentUploadJob(
        UUID documentId,
        byte[] content,
        String originalFileName,
        String contentType,
        String courseCode,
        String courseName,
        String chapterCode,
        String chapterTitle,
        String conversationSessionId,
        String checksum,
        ChunkingStrategy chunkingStrategy
) {}
