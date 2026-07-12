package com.ragchatbot.domain.model;
import java.util.List;

// /**
//  * Đại diện cho một câu hỏi kiểm thử (test case) dùng để đánh giá hiệu năng của chatbot.
//  * * @param id Mã định danh duy nhất của câu hỏi (Ví dụ: "Q001").
//  * @param question Nội dung câu hỏi người dùng đặt ra.
//  * @param groundTruth Đáp án chuẩn xác tuyệt đối được trích xuất từ tài liệu, dùng làm hệ quy chiếu.
//  * @param category Phân loại câu hỏi phục vụ việc thống kê điểm mạnh/yếu.
//  */

public record TestCase(
        String id,
        String question,
        String groundTruth,
        String category,
        String expectedSource,
        List<String> expectedKeywords,
        List<RelevantSource> relevantSources, // Trường mới 
        Boolean outOfScope
) {}