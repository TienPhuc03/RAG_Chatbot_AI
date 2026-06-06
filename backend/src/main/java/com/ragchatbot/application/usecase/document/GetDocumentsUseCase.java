package com.ragchatbot.application.usecase.document;

import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetDocumentsUseCase {

    private final DocumentRepository documentRepository;

    public GetDocumentsUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Lấy danh sách document theo courseCode.
     */
    public List<Document> execute(String courseCode) {
        return documentRepository.findByCourseCode(courseCode);
    }
}