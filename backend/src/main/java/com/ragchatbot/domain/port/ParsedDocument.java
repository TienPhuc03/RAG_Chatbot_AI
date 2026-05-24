package com.ragchatbot.domain.port;

import java.util.Map;

public record ParsedDocument(
        String title,
        String rawText,
        String sourceFileName,
        String contentType,
        Map<String, String> metadata
) {
}
