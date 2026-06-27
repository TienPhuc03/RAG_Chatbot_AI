package com.ragchatbot.application.usecase.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ragchatbot.application.dto.chat.ConversationSummaryDto;
import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetConversationsUseCaseTest {

    @Test
    void executeMapsConversationsInUpdatedOrder() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);

        Conversation first = new Conversation();
        first.setId(UUID.randomUUID());
        first.setSessionId("session-newest");
        first.setTitle("Newest conversation");
        first.setCreatedAt(Instant.parse("2026-06-27T10:15:30Z"));
        first.setUpdatedAt(Instant.parse("2026-06-27T10:20:30Z"));

        Conversation second = new Conversation();
        second.setId(UUID.randomUUID());
        second.setSessionId("session-older");
        second.setTitle("Older conversation");
        second.setCreatedAt(Instant.parse("2026-06-26T10:15:30Z"));
        second.setUpdatedAt(Instant.parse("2026-06-26T10:20:30Z"));

        when(conversationRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(first, second));

        GetConversationsUseCase useCase = new GetConversationsUseCase(conversationRepository);

        List<ConversationSummaryDto> result = useCase.execute();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ConversationSummaryDto::sessionId)
                .containsExactly("session-newest", "session-older");
        assertThat(result.get(0).updatedAt()).isEqualTo(Instant.parse("2026-06-27T10:20:30Z"));
    }
}
