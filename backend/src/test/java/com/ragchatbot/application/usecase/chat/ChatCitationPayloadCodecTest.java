package com.ragchatbot.application.usecase.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.application.dto.chat.ChatCitationDto;
import com.ragchatbot.domain.port.CitationReference;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatCitationPayloadCodecTest {

    private final ChatCitationPayloadCodec codec = new ChatCitationPayloadCodec(new ObjectMapper());

    @Test
    void serializesAndDeserializesStructuredCitations() {
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        List<ChatCitationDto> citations = codec.toDtos(List.of(
                new CitationReference(documentId, chunkId, "oop.pdf", 9, "JAVA101", "CH1", 0.88)
        ));

        String payload = codec.serialize(citations);
        List<ChatCitationDto> decoded = codec.deserialize(payload);

        assertThat(decoded).containsExactly(
                new ChatCitationDto(documentId, chunkId, "oop.pdf", 9, "JAVA101", "CH1", 0.88)
        );
    }

    @Test
    void deserializesLegacyStringCitationPayload() {
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        List<ChatCitationDto> decoded = codec.deserialize("[\"" + documentId + ":" + chunkId + "\"]");

        assertThat(decoded).containsExactly(
                new ChatCitationDto(documentId, chunkId, null, null, null, null, null)
        );
    }
}
