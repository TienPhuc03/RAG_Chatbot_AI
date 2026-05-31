package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByChecksum(String checksum);

    List<Document> findByCourseCode(String courseCode);

    List<Document> findByCourseCodeAndChapterCode(
            String courseCode,
            String chapterCode
    );
}