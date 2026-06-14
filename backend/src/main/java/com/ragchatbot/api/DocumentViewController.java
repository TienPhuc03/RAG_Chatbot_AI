package com.ragchatbot.api;

import com.ragchatbot.application.usecase.document.GetDocumentsUseCase;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller render trang HTML quản lý tài liệu (Thymeleaf).
 * Khác DocumentController (trả JSON), controller này trả tên template HTML.
 * Truy cập tại: GET /documents
 */
@Controller
@RequestMapping("/documents")
public class DocumentViewController {

    private final GetDocumentsUseCase getDocumentsUseCase;
    private final ChunkRepository chunkRepository;

    public DocumentViewController(
            GetDocumentsUseCase getDocumentsUseCase,
            ChunkRepository chunkRepository
    ) {
        this.getDocumentsUseCase = getDocumentsUseCase;
        this.chunkRepository = chunkRepository;
    }

    /**
     * Render trang danh sách tài liệu.
     * Truyền documents + chunkCounts vào template để hiển thị bảng.
     *
     * @param courseCode mã môn học, mặc định "JAVA101"
     * @param model      object truyền dữ liệu xuống Thymeleaf template
     * @return tên template → resources/templates/documents.html
     */
    @GetMapping
    public String listDocuments(
            @RequestParam(defaultValue = "JAVA101") String courseCode,
            Model model
    ) {
        // Lấy danh sách tài liệu theo môn học
        List<Document> docs = getDocumentsUseCase.execute(courseCode);

        // Đếm chunk của từng document: Map<documentId, chunkCount>
        Map<UUID, Long> chunkCounts = docs.stream()
                .collect(Collectors.toMap(
                        Document::getId,
                        d -> chunkRepository.countByDocumentId(d.getId())
                ));

        // Truyền dữ liệu xuống template
        model.addAttribute("documents", docs);
        model.addAttribute("courseCode", courseCode);
        model.addAttribute("chunkCounts", chunkCounts);

        return "documents";
    }
}