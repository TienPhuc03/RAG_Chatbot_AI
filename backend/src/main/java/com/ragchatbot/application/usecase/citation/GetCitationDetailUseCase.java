package com.ragchatbot.application.usecase.citation;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ragchatbot.application.dto.citation.CitationDetailResponse;
import com.ragchatbot.domain.model.Chunk;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;

/*
 *Use case: lấy chi tiết một chunk được trích dẫn (citation) để hiển thị trong modal.
 *
 *Flow:
 *   FE click citation chip → gọi GET /api/citations/{chunkId}
 *   → use case này lấy Chunk + Document liên kết
 *   → trả về content, tên file, số trang cho modal hiển thị
 */
@Service
public class GetCitationDetailUseCase {

    private final ChunkRepository chunkRepository;

    public GetCitationDetailUseCase(ChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    /*
     *Lấy chi tiết chunk theo ID để hiển thị trong citation modal.
     *
     *@param chunkId UUID của chunk cần xem chi tiết
     *@return CitationDetailResponse chứa content, tên file, số trang
     *@throws RuntimeException nếu chunk không tồn tại
     */
    public CitationDetailResponse execute(UUID chunkId) {

        Chunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new RuntimeException("Chunk not found: " + chunkId));

        // Lấy document cha để hiển thị tên file và tiêu đề
        Document document = chunk.getDocument();

        return new CitationDetailResponse(
                chunk.getId(),
                chunk.getContent(),
                document.getSourceFileName(),
                chunk.getPageNumber(),
                document.getTitle()
        );
    }
}