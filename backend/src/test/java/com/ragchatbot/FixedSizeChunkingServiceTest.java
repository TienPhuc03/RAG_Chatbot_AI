package com.ragchatbot;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.infrastructure.chunking.FixedSizeChunkingService;

class FixedSizeChunkingServiceTest {

    private final FixedSizeChunkingService chunkingService = new FixedSizeChunkingService();

    @Test
    void testVerifyChunkCountAndNoCharacterLoss() {
        // Chuỗi mẫu gồm 30 ký tự tiếng Anh để test thuật toán cơ bản
        String text = "abcdefghijklmnopqrstuvwxyz1234";
        
        // Cấu hình kích thước chunk = 15, độ gối đầu (overlap) = 5
        ChunkingOptions options = new ChunkingOptions(15, 5);
        
        List<ChunkDraft> chunks = chunkingService.chunk(text, ChunkingStrategy.FIXED_SIZE, options);

        // 1. [Yêu cầu: Verify chunk count] - Đảm bảo danh sách không rỗng và chia làm nhiều đoạn
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(1);

        // 2. [Yêu cầu: Không mất ký tự] - Ghép nội dung các chunk lại (sau khi loại bỏ khoảng gối đầu)
        StringBuilder fullTextBuilder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i).content();
            if (i == 0) {
                fullTextBuilder.append(content);
            } else {
                // Đoạn sau chỉ lấy phần text mới, bỏ qua 5 ký tự gối đầu (overlap) đã có ở chunk trước
                fullTextBuilder.append(content.substring(5));
            }
        }
        // Kết quả ghép lại phải khớp hoàn toàn với chuỗi gốc ban đầu
        assertThat(fullTextBuilder.toString()).isEqualTo(text);
    }

    @Test
    void testVerifyOverlap() {
        String text = "abcdefghijklmnopqrstuvwxyz1234";
        ChunkingOptions options = new ChunkingOptions(15, 5); // size 15, overlap 5
        
        List<ChunkDraft> chunks = chunkingService.chunk(text, ChunkingStrategy.FIXED_SIZE, options);
        
        // [Yêu cầu: Overlap chính xác] - Kiểm tra tính gối đầu của đoạn văn bản
        if (chunks.size() > 1) {
            String chunk0 = chunks.get(0).content(); // Đoạn đầu tiên
            String chunk1 = chunks.get(1).content(); // Đoạn kế tiếp
            
            // Lấy 5 ký tự cuối cùng của chunk thứ nhất
            String expectedOverlap = chunk0.substring(chunk0.length() - 5);
            
            // Đoạn thứ hai phải được bắt đầu bằng chính xác phần gối đầu đó
            assertThat(chunk1.startsWith(expectedOverlap)).isTrue();
        }
    }

    @Test
    void testVietnameseToneMarks() {
    // Yêu cầu: tiếng Việt phải được giữ nguyên, không mất dữ liệu sau chunking
    String vietnameseText =
            "Học lập trình Java Spring Boot, tiếng Việt giữ nguyên dấu hoàn toàn.";

    ChunkingOptions options = new ChunkingOptions(50, 5);

    List<ChunkDraft> chunks =
            chunkingService.chunk(
                    vietnameseText,
                    ChunkingStrategy.FIXED_SIZE,
                    options);

    // Có tạo ra chunk
    assertThat(chunks).isNotEmpty();
    assertThat(chunks.size()).isGreaterThan(1);

    // Ghép lại nội dung sau khi loại bỏ overlap
    StringBuilder reconstructed = new StringBuilder();

    for (int i = 0; i < chunks.size(); i++) {
        String content = chunks.get(i).content();

        if (i == 0) {
            reconstructed.append(content);
        } else {
            reconstructed.append(content.substring(5)); // overlap = 5
        }
    }

    // Không mất dữ liệu tiếng Việt
    assertThat(reconstructed.toString())
            .isEqualTo(vietnameseText);

    // Kiểm tra các cụm từ tiếng Việt vẫn tồn tại
    assertThat(reconstructed.toString())
            .contains("Học");

    assertThat(reconstructed.toString())
            .contains("tiếng Việt");

    assertThat(reconstructed.toString())
            .contains("giữ nguyên");

    assertThat(reconstructed.toString())
            .contains("dấu hoàn toàn");
}

}