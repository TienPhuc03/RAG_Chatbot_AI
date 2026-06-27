package com.ragchatbot.application.usecase.chat;

import com.ragchatbot.application.dto.chat.ConversationSummaryDto;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetConversationsUseCase {

    private final ConversationRepository conversationRepository;

    public GetConversationsUseCase(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public List<ConversationSummaryDto> execute() {
        return conversationRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(conversation -> new ConversationSummaryDto(
                        conversation.getId(),
                        conversation.getSessionId(),
                        conversation.getTitle(),
                        conversation.getCreatedAt(),
                        conversation.getUpdatedAt()
                ))
                .toList();
    }
}
