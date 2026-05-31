package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository
        extends JpaRepository<Message, UUID> {

    List<Message> findByConversationId(UUID conversationId);
}