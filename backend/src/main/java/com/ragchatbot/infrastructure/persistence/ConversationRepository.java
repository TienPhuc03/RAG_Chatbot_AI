package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findBySessionId(String sessionId);
}
