package com.ragchatbot.application.usecase.chat;

import com.ragchatbot.application.dto.chat.ChatHistoryMessageDto;
import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GetChatHistoryUseCase {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatCitationPayloadCodec citationPayloadCodec;

    public GetChatHistoryUseCase(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ChatCitationPayloadCodec citationPayloadCodec
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.citationPayloadCodec = citationPayloadCodec;
    }

    /**
     * Lấy lịch sử hội thoại theo sessionId.
     */
    public List<ChatHistoryMessageDto> execute(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return List.of();
        }

        Conversation conversation =
                conversationRepository.findBySessionId(sessionId.trim())
                        .orElse(null);

        if (conversation == null) {
            return List.of();
        }

        return messageRepository
                .findByConversationIdOrderBySequenceNoAsc(
                        conversation.getId()
                )
                .stream()
                .map(message -> {
                    var citations = citationPayloadCodec.deserialize(message.getCitationPayload());
                    return new ChatHistoryMessageDto(
                            message.getId(),
                            message.getRole(),
                            message.getContent(),
                            message.getCreatedAt(),
                            !citations.isEmpty(),
                            message.getCitationPayload(),
                            citations
                    );
                })
                .toList();
    }
}
