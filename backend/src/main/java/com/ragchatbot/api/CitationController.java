package com.ragchatbot.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ragchatbot.application.dto.citation.CitationDetailResponse;
import com.ragchatbot.application.usecase.citation.GetCitationDetailUseCase;

/*
 * Controller cho citation modal.
 * FE gọi endpoint này khi người dùng click vào citation chip
 * trong câu trả lời để xem đoạn văn bản gốc.
 */
@RestController
@RequestMapping("/api/citations")
public class CitationController {

    private final GetCitationDetailUseCase getCitationDetailUseCase;

    public CitationController(GetCitationDetailUseCase getCitationDetailUseCase) {
        this.getCitationDetailUseCase = getCitationDetailUseCase;
    }

    /*
     * Lấy chi tiết một citation (chunk) để hiển thị trong modal.
     *
     * @param chunkId UUID của chunk, lấy từ citations[] trong câu trả lời chatbot
     * @return content gốc, tên file, số trang
     */
    @GetMapping("/{chunkId}")
    public CitationDetailResponse getCitationDetail(@PathVariable UUID chunkId) {
        return getCitationDetailUseCase.execute(chunkId);
    }
}