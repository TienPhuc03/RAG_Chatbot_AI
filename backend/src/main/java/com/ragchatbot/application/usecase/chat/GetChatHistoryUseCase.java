package com.ragchatbot.application.usecase.chat;

import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.domain.model.Message;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<Message> execute(String sessionId) {

        Conversation conversation =
                conversationRepository.findBySessionId(sessionId)
                        .orElseThrow();

        return messageRepository
                .findByConversationIdOrderBySequenceNoAsc(
                        conversation.getId()
                );
    }
}