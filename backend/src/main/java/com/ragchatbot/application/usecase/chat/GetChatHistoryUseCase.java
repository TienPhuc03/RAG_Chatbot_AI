package com.ragchatbot.application.usecase.chat;

import com.ragchatbot.application.dto.chat.ChatHistoryMessageDto;
import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetChatHistoryUseCase {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public GetChatHistoryUseCase(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Lấy lịch sử hội thoại theo sessionId.
     */
    public List<ChatHistoryMessageDto> execute(String sessionId) {

        Conversation conversation =
                conversationRepository.findBySessionId(sessionId)
                        .orElseThrow();

        return messageRepository
                .findByConversationIdOrderBySequenceNoAsc(
                        conversation.getId()
                )
                .stream()
                .map(message -> new ChatHistoryMessageDto(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt(),
                        message.getCitationPayload()
                ))
                .toList();
    }
}
