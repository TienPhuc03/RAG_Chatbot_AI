package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Message;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {
}
