package com.ragchatbot.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ragchatbot.domain.model.Document;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByChecksum(String checksum);

    List<Document> findByCourseCode(String courseCode);

    List<Document> findByCourseCodeAndChapterCode(
            String courseCode,
            String chapterCode
    );
    //Sort theo created_at giảm dần (mới nhất trước)
    //Spring Data tự sinh query từ tên method, không cần viết SQL
    List<Document> findByCourseCodeOrderByCreatedAtDesc(String courseCode);

    //Lấy tất cả document, sort theo created_at — dùng cho dropdown "Tất cả môn học"
    List<Document> findAllByOrderByCreatedAtDesc();
}