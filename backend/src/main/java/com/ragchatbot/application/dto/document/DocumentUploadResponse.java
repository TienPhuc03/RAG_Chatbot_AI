package com.ragchatbot.application.dto.document;

import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.enums.EmbeddingModel;
import java.util.UUID;

public record DocumentUploadResponse(
        UUID id,
        String title,
        String sourceFileName,
        String courseCode,
        DocumentStatus status,
        EmbeddingModel embeddingModel
) {
}
