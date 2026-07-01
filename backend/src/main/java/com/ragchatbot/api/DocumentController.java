package com.ragchatbot.api;

import com.ragchatbot.application.dto.document.DocumentChunkPreviewResponse;
import com.ragchatbot.application.dto.document.DocumentListResponse;
import com.ragchatbot.application.dto.document.DocumentStatusResponse;
import com.ragchatbot.application.dto.document.DocumentUploadResponse;
import com.ragchatbot.application.usecase.document.GetDocumentChunksUseCase;
import com.ragchatbot.application.usecase.document.GetDocumentStatusUseCase;
import com.ragchatbot.application.usecase.document.GetDocumentsUseCase;
import com.ragchatbot.application.usecase.document.UploadDocumentUseCase;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetDocumentsUseCase getDocumentsUseCase;
    private final GetDocumentStatusUseCase getDocumentStatusUseCase;
    private final GetDocumentChunksUseCase getDocumentChunksUseCase;

    public DocumentController(
            UploadDocumentUseCase uploadDocumentUseCase,
            GetDocumentsUseCase getDocumentsUseCase,
            GetDocumentStatusUseCase getDocumentStatusUseCase,
            GetDocumentChunksUseCase getDocumentChunksUseCase
    ) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.getDocumentsUseCase = getDocumentsUseCase;
        this.getDocumentStatusUseCase = getDocumentStatusUseCase;
        this.getDocumentChunksUseCase = getDocumentChunksUseCase;
    }

    @Operation(summary = "Upload document")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Upload accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid file")
    })
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @Parameter(description = "File PDF, DOCX hoac PPTX can upload")
            @RequestParam("file") MultipartFile file,
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

    @GetMapping
    public ResponseEntity<List<DocumentListResponse>> getDocuments(
            @RequestParam(required = false) String courseCode
    ) {
        return ResponseEntity.ok(getDocumentsUseCase.execute(courseCode));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(getDocumentStatusUseCase.execute(id));
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkPreviewResponse>> getDocumentChunks(@PathVariable UUID id) {
        return ResponseEntity.ok(getDocumentChunksUseCase.execute(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }
}
