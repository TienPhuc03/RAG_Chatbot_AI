package com.ragchatbot.application.usecase.document;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;

@Service
public class GetDocumentsUseCase {

    private final DocumentRepository documentRepository;

    public GetDocumentsUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /*
     * Lấy danh sách document theo courseCode.sort theo created_at mới nhất trước.
     * Nếu courseCode = "ALL" hoặc rỗng, trả về toàn bộ document.
     */
     
    public List<Document> execute(String courseCode) {
         // courseCode = "ALL" nghĩa là FE muốn xem tất cả môn học (dropdown filter)
        if (courseCode == null || courseCode.isBlank() || courseCode.equalsIgnoreCase("ALL")) {
        return documentRepository.findAllByOrderByCreatedAtDesc();
        } else {
            return documentRepository.findByCourseCodeOrderByCreatedAtDesc(courseCode);
        }
    }
}