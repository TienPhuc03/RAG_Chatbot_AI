package com.ragchatbot.application.usecase.document;

import java.util.UUID;

record DocumentUploadJob(
        UUID documentId,
        byte[] content,
        String originalFileName,
        String contentType,
        String courseCode,
        String courseName,
        String chapterCode,
        String chapterTitle,
        String checksum
) {
}
