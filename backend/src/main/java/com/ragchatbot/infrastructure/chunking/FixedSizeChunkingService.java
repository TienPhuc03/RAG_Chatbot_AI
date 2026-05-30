package com.ragchatbot.infrastructure.chunking;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.domain.port.ChunkingService;

@Service
public class FixedSizeChunkingService implements ChunkingService {

    @Override
    public List<ChunkDraft> chunk(String rawText, ChunkingStrategy strategy, ChunkingOptions options) {
        List<ChunkDraft> chunks = new ArrayList<>();

        if (rawText == null || rawText.trim().isEmpty()) {
            return chunks;
        }

        // Đọc thông số chính xác từ Record ChunkingOptions của nhóm
        int chunkSize = (options != null && options.chunkSize() > 0) ? options.chunkSize() : 512;
        int chunkOverlap = (options != null && options.chunkOverlap() > 0) ? options.chunkOverlap() : 50;

        int start = 0;
        int textLength = rawText.length();
        int currentIndex = 0;

        // Thuật toán cắt chuỗi gối đầu theo ký tự thực tế
        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            
            String chunkContent = rawText.substring(start, end);
            int tokenCount = chunkContent.length();

            // Khởi tạo đúng cấu trúc Record ChunkDraft: chunkIndex, content, pageNumber, tokenCount
            ChunkDraft draft = new ChunkDraft(
                currentIndex++,
                chunkContent,
                1,
                tokenCount
            );
            chunks.add(draft);

            if (end == textLength) {
                break;
            }

            // Điểm bắt đầu tiếp theo lùi lại bằng khoảng gối đầu chunkOverlap
            start = end - chunkOverlap;

            // Chống lặp vô hạn nếu cấu hình sai lỗi gối đầu lớn hơn độ dài chunk
            if (start >= end) {
                start = end;
            }
        }

        return chunks;
    }
}
