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

    /*
     *Upload tài liệu để indexing.
    /**
     * API upload tài liệu vào hệ thống RAG.
     *
     * Hỗ trợ:
     * - PDF
     * - DOCX
     * - PPTX
     *
     * Sau khi upload, hệ thống sẽ thực hiện:
     * Parse -> Chunk -> Embed -> Index Vector Store
     */
    @Operation(
            summary = "Upload document",
            description = "Upload tài liệu để hệ thống RAG thực hiện parsing và indexing."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Upload successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file"
            )
    })
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(

            @Parameter(
                    description = "File PDF, DOCX hoặc PPTX cần upload"
            )

            @RequestParam("file")
            MultipartFile file,

            @RequestParam String courseCode,
            @RequestParam String courseName,
            @RequestParam(required = false, defaultValue = "") String chapterCode,
            @RequestParam(required = false, defaultValue = "") String chapterTitle

    ) {
        uploadDocumentUseCase.execute(file);
        return ResponseEntity.ok().build();
    }

    /*
     *Lấy danh sách tài liệu theo courseCode.
    /**
     * API lấy danh sách tài liệu theo môn học.
     *
     * Ví dụ:
     * courseCode = JAVA101
     *
     * Trả về:
     * Danh sách Document thuộc môn học đó.
     */
    @Operation(
            summary = "Get documents",
            description = "Lấy danh sách tài liệu theo courseCode."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Get documents successfully"
            )
    })
    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(

            @Parameter(
                    description = "Mã môn học",
                    example = "JAVA101"
            )

            @RequestParam(required = false)
            String courseCode

    ) {
        List<Document> documents = getDocumentsUseCase.execute(courseCode);
        return ResponseEntity.ok(documents);
    }

    /*
 * FE gọi endpoint này mỗi 2s để cập nhật badge trạng thái.
 * Trả PROCESSING khi đang xử lý, INDEXED khi xong, FAILED khi lỗi.
 */
@GetMapping("/{id}/status")
public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable UUID id) {
        DocumentStatusResponse response = getDocumentStatusUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    /*
     * API Xóa tài liệu theo ID.
     */
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
        //tùy biến: Sau này bạn có thể tạo thêm `DeleteDocumentUseCase` và gọi ở đây.
        //hiện tại trả về 204 No Content tạm thời đúng chuẩn thiết kế REST.
        return ResponseEntity.noContent().build(); 
    }
}