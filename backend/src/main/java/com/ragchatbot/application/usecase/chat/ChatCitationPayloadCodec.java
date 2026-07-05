package com.ragchatbot.application.usecase.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.application.dto.chat.ChatCitationDto;
import com.ragchatbot.domain.port.CitationReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatCitationPayloadCodec {

    private static final Logger log = LoggerFactory.getLogger(ChatCitationPayloadCodec.class);

    private final ObjectMapper objectMapper;

    public ChatCitationPayloadCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ChatCitationDto> toDtos(List<CitationReference> citations) {
        if (citations == null || citations.isEmpty()) {
            return List.of();
        }
        return citations.stream()
                .map(citation -> new ChatCitationDto(
                        citation.documentId(),
                        citation.chunkId(),
                        citation.sourceFileName(),
                        citation.pageNumber(),
                        citation.courseCode(),
                        citation.chapterCode(),
                        citation.score()
                ))
                .toList();
    }

    public String serialize(List<ChatCitationDto> citations) {
        try {
            return objectMapper.writeValueAsString(citations == null ? List.of() : citations);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize citation payload", ex);
            return "[]";
        }
    }

    public List<ChatCitationDto> deserialize(String citationPayload) {
        if (!StringUtils.hasText(citationPayload)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(citationPayload);
            if (!root.isArray()) {
                return List.of();
            }

            List<ChatCitationDto> citations = new ArrayList<>();
            for (JsonNode node : root) {
                if (node.isTextual()) {
                    citations.add(parseLegacyCitation(node.asText()));
                    continue;
                }

                citations.add(new ChatCitationDto(
                        uuidOrNull(node.path("documentId").asText(null)),
                        uuidOrNull(node.path("chunkId").asText(null)),
                        textOrNull(node, "sourceFileName"),
                        node.path("pageNumber").isNumber() ? node.path("pageNumber").intValue() : null,
                        textOrNull(node, "courseCode"),
                        textOrNull(node, "chapterCode"),
                        node.path("score").isNumber() ? node.path("score").doubleValue() : null
                ));
            }
            return citations;
        } catch (Exception ex) {
            log.warn("Failed to deserialize citation payload", ex);
            return List.of();
        }
    }

    private ChatCitationDto parseLegacyCitation(String value) {
        if (!StringUtils.hasText(value)) {
            return new ChatCitationDto(null, null, null, null, null, null, null);
        }

        String[] parts = value.split(":");
        UUID documentId = parts.length > 0 ? uuidOrNull(parts[0]) : null;
        UUID chunkId = parts.length > 1 ? uuidOrNull(parts[1]) : null;
        return new ChatCitationDto(documentId, chunkId, null, null, null, null, null);
    }

    private UUID uuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText() : null;
    }
}
