package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderBySequenceNoAsc(UUID conversationId);

    List<Message> findTop5ByConversationIdOrderBySequenceNoDesc(UUID conversationId);
}