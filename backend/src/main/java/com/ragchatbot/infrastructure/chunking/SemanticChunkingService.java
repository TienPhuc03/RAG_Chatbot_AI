package com.ragchatbot.infrastructure.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.domain.port.ChunkingService;

@Service

public class SemanticChunkingService implements ChunkingService {

    /** Câu dài hơn ngưỡng này sẽ bị fallback sang FixedSize thay vì giữ nguyên 1 chunk. */
    private static final int FALLBACK_THRESHOLD = 800;

    /** Giá trị mặc định cho FixedSize fallback khi options không được truyền vào. */
    private static final int DEFAULT_FALLBACK_CHUNK_SIZE    = 512;
    private static final int DEFAULT_FALLBACK_CHUNK_OVERLAP = 50;

    /**
     * Regex phát hiện ranh giới câu tiếng Việt:
     *   - Group 1 [.!?。！？]+ : một hoặc nhiều dấu kết câu liên tiếp (vd: "??!", "...")
     *   - Group 2 [\s]+|$      : khoảng trắng theo sau HOẶC cuối chuỗi
     * Yêu cầu có khoảng trắng sau dấu câu để tránh cắt nhầm số thập phân (vd: "3.14").
     */
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
        "([.!?。！？]+)([\\s]+|$)"
    );

    /**
     * Điểm vào chính của service.
     * Nhận rawText → tách câu → kiểm tra độ dài → tạo ChunkDraft → trả về danh sách.
     */
    @Override
    public List<ChunkDraft> chunk(String rawText, ChunkingStrategy strategy, ChunkingOptions options) {
        List<ChunkDraft> result = new ArrayList<>();

        if (rawText == null || rawText.trim().isEmpty()) {
            return result;
        }

        List<String> sentences = splitIntoSentences(rawText);

        int chunkIndex = 0;
        for (String sentence : sentences) {
            if (sentence.isBlank()) continue;

            if (sentence.length() > FALLBACK_THRESHOLD) {
                // Câu quá dài → chia nhỏ tiếp bằng FixedSize để tránh chunk khổng lồ
                List<ChunkDraft> subChunks = fixedSizeFallback(sentence, chunkIndex, options);
                result.addAll(subChunks);
                chunkIndex += subChunks.size();
            } else {
                // Câu bình thường → mỗi câu là 1 chunk độc lập
                result.add(new ChunkDraft(
                    chunkIndex++,
                    sentence.trim(),
                    1,                       // pageNumber mặc định = 1
                    sentence.trim().length() // tokenCount xấp xỉ bằng số ký tự
                ));
            }
        }

        return result;
    }

    /**
     * Tách văn bản thành danh sách câu dựa trên SENTENCE_BOUNDARY regex.
     * Dấu kết câu được giữ lại ở cuối mỗi câu (không bị xóa).
     * Phần văn bản cuối không có dấu câu vẫn được thu thập như 1 câu riêng.
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_BOUNDARY.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            // Lấy từ vị trí trước đến hết dấu kết câu (group 1), bỏ khoảng trắng (group 2)
            String sentence = text.substring(lastEnd, matcher.end(1)).strip();
            if (!sentence.isEmpty()) sentences.add(sentence);
            lastEnd = matcher.end(); // nhảy qua cả khoảng trắng, bắt đầu câu mới
        }

        // Thu thập phần đuôi nếu văn bản không kết thúc bằng dấu câu
        if (lastEnd < text.length()) {
            String tail = text.substring(lastEnd).strip();
            if (!tail.isEmpty()) sentences.add(tail);
        }

        return sentences;
    }

    /**
     * Fallback FixedSize: dùng khi 1 câu vượt quá FALLBACK_THRESHOLD ký tự.
     * Thuật toán cắt gối đầu (sliding window) giống FixedSizeChunkingService
     * nhưng được implement nội bộ để đảm bảo tính độc lập hoàn toàn của class này.
     *
     * @param text        câu dài cần chia nhỏ
     * @param startIndex  chunkIndex bắt đầu, đảm bảo index liên tục với các chunk trước
     * @param options     cấu hình chunkSize và chunkOverlap; dùng default nếu null
     */
    private List<ChunkDraft> fixedSizeFallback(String text, int startIndex, ChunkingOptions options) {
        List<ChunkDraft> chunks = new ArrayList<>();

        int chunkSize    = (options != null && options.chunkSize()    > 0) ? options.chunkSize()    : DEFAULT_FALLBACK_CHUNK_SIZE;
        int chunkOverlap = (options != null && options.chunkOverlap() > 0) ? options.chunkOverlap() : DEFAULT_FALLBACK_CHUNK_OVERLAP;

        int start        = 0;
        int textLength   = text.length();
        int currentIndex = startIndex;

        while (start < textLength) {
            int end        = Math.min(start + chunkSize, textLength);
            String content = text.substring(start, end);

            chunks.add(new ChunkDraft(currentIndex++, content, 1, content.length()));

            if (end == textLength) break;

            // Điểm bắt đầu tiếp theo lùi lại đúng bằng chunkOverlap để tạo vùng gối đầu
            start = end - chunkOverlap;
            if (start >= end) start = end; // bảo vệ tránh vòng lặp vô hạn nếu overlap >= chunkSize
        }

        return chunks;
    }
}