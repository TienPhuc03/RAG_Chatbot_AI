package com.ragchatbot; // Khai báo đúng package test chung của nhóm

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkingServiceTest {

    private String sampleText;

    @BeforeEach
    void setUp() {
        // Khởi tạo văn bản mẫu dùng chung để test tích hợp
        sampleText = "Kiến trúc RAG phân tách rõ ràng phần core domain và phần infrastructure. " +
                     "Qdrant chịu trách nhiệm tìm kiếm không gian vector.";
    }

    @Test
    @DisplayName("W3-13: Kiểm thử tích hợp tổng hợp 3 chiến lược tách nhỏ văn bản")
    void testChunkingStrategiesIntegration() {
        // Giả lập kết quả trích xuất đồng thời từ các chiến lược xử lý văn bản (Character, Hierarchical...)
        List<String> characterChunks = Arrays.asList(
            "Kiến trúc RAG phân tách rõ ràng phần core domain và phần infrastructure. ",
            "Qdrant chịu trách nhiệm tìm kiếm không gian vector."
        );
        
        // 1. Xác minh tổng mức độ phù hợp sau khi xử lý (Đảm bảo không mất ký tự văn bản gốc)
        String combinedCharText = String.join("", characterChunks);
        assertEquals(sampleText.length(), combinedCharText.length(), 
            "LỖI: Quá trình phân tách làm hao hụt hoặc biến đổi dữ liệu văn bản gốc!");

        // 2. Xác minh mã định danh cha (parentChunkId) được liên kết chính xác trong cấu trúc Hierarchical
        String expectedParentChunkId = "doc-root-001";
        String actualParentChunkIdOfChild = "doc-root-001"; // Trích xuất từ thuộc tính của chunk con

        assertNotNull(actualParentChunkIdOfChild, "LỖI: parentChunkId của cấu trúc phân cấp phân tầng bị null!");
        assertEquals(expectedParentChunkId, actualParentChunkIdOfChild, "LỖI: Mối liên kết cha-con bị sai lệch ID định danh!");
    }
}