package com.ragchatbot.api;

import com.ragchatbot.application.usecase.document.GetDocumentStatusUseCase;
import com.ragchatbot.application.usecase.document.GetDocumentsUseCase;
import com.ragchatbot.application.usecase.document.UploadDocumentUseCase;   
import com.ragchatbot.domain.model.Document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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
    public void uploadDocument(

            @Parameter(
                    description = "File PDF, DOCX hoặc PPTX cần upload"
            )

            @RequestParam("file")
            MultipartFile file

    ) {
        uploadDocumentUseCase.execute(file);
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
    public List<Document> getDocuments(

            @Parameter(
                    description = "Mã môn học",
                    example = "JAVA101"
            )

            @RequestParam
            String courseCode

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