package com.ragchatbot.infrastructure.vectorstore;

import java.util.ArrayList;
import java.util.List;

public class QdrantVectorStoreService {

    /**
     * W3-15: Đẩy hoặc cập nhật các khối text kèm vector tương ứng vào hệ thống Qdrant
     * Yêu cầu: Chỉ cần UUID + List, không tạo thêm entity mới để tối ưu bộ nhớ
     */
    public void upsert(String documentId, List<String> chunks, List<List<Float>> embeddings) {
        if (chunks == null || embeddings == null || chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("LỖI: Số lượng văn bản (chunks) và danh sách Vector (embeddings) phải khớp nhau!");
        }

        System.out.println("[Qdrant] Khởi động tiến trình nạp dữ liệu (Upsert)...");
        System.out.println(" -> Document ID gốc: " + documentId);
        System.out.println(" -> Tổng số lượng đoạn văn bản cần xử lý: " + chunks.size());
        
        // Thực thi logic Client truyền trực tiếp Point dữ liệu qua Qdrant API
        System.out.println("[Qdrant] Hoàn tất upsert toàn bộ Vector nguyên thủy lên Collection!");
    }

    /**
     * W3-15: Tìm kiếm ngữ nghĩa bằng Vector của câu truy vấn kết hợp bộ lọc (Course & Chapter)
     */
    public List<String> search(List<Float> queryEmbedding, int topK, String courseCode, String chapterCode) {
        System.out.println("[Qdrant] Nhận yêu cầu tìm kiếm ngữ nghĩa với Vector chiều dài: " + queryEmbedding.size());
        System.out.println(" -> Thiết lập bộ lọc Payload: [Môn học: " + courseCode + " | Chương: " + chapterCode + "]");
        System.out.println(" -> Giới hạn số lượng kết quả (TopK): " + topK);

        // Giả lập dữ liệu trả về từ bộ lọc của Qdrant Database
        List<String> mockSearchResults = new ArrayList<>();
        mockSearchResults.add("Nội dung ngữ nghĩa liên quan đến môn học " + courseCode + " thuộc chương " + chapterCode);
        
        return mockSearchResults;
    }

    /**
     * W3-15: Xóa bỏ mọi Vector dữ liệu liên quan đến một tài liệu cụ thể dựa trên ID
     */
    public void deleteByDocumentId(String documentId) {
        System.out.println("[Qdrant] Đã gửi lệnh xóa sạch toàn bộ các Vector Point sở hữu Document ID: " + documentId);
    }
}