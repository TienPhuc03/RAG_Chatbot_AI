package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.enums.DocumentStatus;
import java.time.Instant;
import com.ragchatbot.domain.model.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByChecksum(String checksum);

    List<Document> findByCourseCode(String courseCode);

    List<Document> findAllByConversationSessionIdIsNullOrderByCreatedAtDesc();

    List<Document> findByCourseCodeAndConversationSessionIdIsNullOrderByCreatedAtDesc(String courseCode);

    List<Document> findByCourseCodeAndChapterCode(
            String courseCode,
            String chapterCode
    );

    List<Document> findByConversationSessionIdOrderByCreatedAtAsc(String conversationSessionId);

    List<Document> findByStatusAndUpdatedAtBefore(DocumentStatus status, Instant updatedAt);

    long countByStatus(DocumentStatus status);

    long countByStatusAndCourseCode(DocumentStatus status, String courseCode);

    long countByStatusAndCourseCodeAndChapterCode(
            DocumentStatus status,
            String courseCode,
            String chapterCode
    );

    long countByStatusAndConversationSessionId(DocumentStatus status, String conversationSessionId);
}
