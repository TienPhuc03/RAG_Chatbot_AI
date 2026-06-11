package com.ragchatbot.api;

import com.ragchatbot.application.usecase.document.GetDocumentStatusUseCase;
import com.ragchatbot.application.usecase.document.GetDocumentsUseCase;
import com.ragchatbot.application.usecase.document.UploadDocumentUseCase;   
import com.ragchatbot.domain.model.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ragchatbot.application.dto.document.DocumentStatusResponse;

import java.util.UUID;


import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetDocumentsUseCase getDocumentsUseCase;
    private final GetDocumentStatusUseCase getDocumentStatusUseCase;


    public DocumentController(
            UploadDocumentUseCase uploadDocumentUseCase,
            GetDocumentsUseCase getDocumentsUseCase,
            GetDocumentStatusUseCase getDocumentStatusUseCase
    ) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.getDocumentsUseCase = getDocumentsUseCase;
        this.getDocumentStatusUseCase = getDocumentStatusUseCase;
    }

    /*
     *Upload tài liệu để indexing.
     */
    @PostMapping("/upload")
    public void uploadDocument(
            @RequestParam("file") MultipartFile file
    ) {
        uploadDocumentUseCase.execute(file);
    }

    /*
     *Lấy danh sách tài liệu theo courseCode.
     */
    @GetMapping
    public List<Document> getDocuments(
            @RequestParam String courseCode
    ) {
        return getDocumentsUseCase.execute(courseCode);
    }

    /*
 *FE gọi endpoint này mỗi 2s để cập nhật badge trạng thái.
 *Trả PROCESSING khi đang xử lý, INDEXED khi xong, FAILED khi lỗi.
 */
@GetMapping("/{id}/status")
public DocumentStatusResponse getDocumentStatus(@PathVariable UUID id) {
    return getDocumentStatusUseCase.execute(id);
}
}