package com.ragchatbot.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ragchatbot.application.dto.document.DocumentStatusResponse;
import com.ragchatbot.application.dto.document.DocumentUploadResponse;
import com.ragchatbot.application.usecase.document.GetDocumentStatusUseCase;
import com.ragchatbot.application.usecase.document.GetDocumentsUseCase;
import com.ragchatbot.application.usecase.document.UploadDocumentUseCase;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.model.Document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


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

    @Operation(
            summary = "Upload document",
            description = "Upload tài liệu để hệ thống RAG thực hiện parsing và indexing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Upload accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid file")
    })
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @Parameter(description = "File PDF, DOCX hoặc PPTX cần upload")
            @RequestParam("file")
            MultipartFile file,
            @RequestParam String courseCode,
            @RequestParam String courseName,
            @RequestParam(required = false, defaultValue = "") String chapterCode,
            @RequestParam(required = false, defaultValue = "") String chapterTitle,
            @RequestParam(required = false, defaultValue = "SEMANTIC") ChunkingStrategy chunkingStrategy
    ) {
        DocumentUploadResponse response = uploadDocumentUseCase.execute(
                file,
                courseCode,
                courseName,
                chapterCode,
                chapterTitle,
                chunkingStrategy
        );
        return ResponseEntity.accepted().body(response);
    }

    @Operation(
            summary = "Get documents",
            description = "Lấy danh sách tài liệu theo courseCode."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Get documents successfully")
    })
    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(
            @Parameter(description = "Mã môn học", example = "JAVA101")
            @RequestParam(required = false)
            String courseCode
    ) {
        List<Document> documents = getDocumentsUseCase.execute(courseCode);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable UUID id) {
        DocumentStatusResponse response = getDocumentStatusUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete document",
            description = "Xóa tài liệu khỏi hệ thống dựa vào ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }
}
