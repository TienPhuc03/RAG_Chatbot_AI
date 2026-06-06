package com.ragchatbot.api;

import com.ragchatbot.application.usecase.document.GetDocumentsUseCase;
import com.ragchatbot.application.usecase.document.UploadDocumentUseCase;
import com.ragchatbot.domain.model.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetDocumentsUseCase getDocumentsUseCase;

    public DocumentController(
            UploadDocumentUseCase uploadDocumentUseCase,
            GetDocumentsUseCase getDocumentsUseCase
    ) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.getDocumentsUseCase = getDocumentsUseCase;
    }

    /**
     * Upload tài liệu để indexing.
     */
    @PostMapping("/upload")
    public void uploadDocument(
            @RequestParam("file") MultipartFile file
    ) {
        uploadDocumentUseCase.execute(file);
    }

    /**
     * Lấy danh sách tài liệu theo courseCode.
     */
    @GetMapping
    public List<Document> getDocuments(
            @RequestParam String courseCode
    ) {
        return getDocumentsUseCase.execute(courseCode);
    }
}