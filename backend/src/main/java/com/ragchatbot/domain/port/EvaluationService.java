package com.ragchatbot.domain.port;

import com.ragchatbot.domain.model.EvaluationResult;
import java.util.List;

/**
 * Giao diện (Port) định nghĩa hợp đồng cho các dịch vụ thực hiện công việc đánh giá câu trả lời của AI.
 */
public interface EvaluationService {

    /**
     * Thực hiện chấm điểm một câu trả lời của hệ thống RAG dựa trên câu hỏi và đáp án chuẩn.
     *
     * @param question Câu hỏi đầu vào.
     * @param groundTruth Đáp án chuẩn xác dùng để đối chiếu.
     * @param answer Câu trả lời thực tế do mô hình AI sinh ra.
     * @param contexts Danh sách các đoạn tài liệu (chunks) được hệ thống trích xuất và cung cấp cho AI làm ngữ cảnh.
     * @return Đối tượng EvaluationResult chứa tổng hợp các điểm số sau khi đánh giá.
     */
    EvaluationResult evaluate(String question, String groundTruth, String answer, List<String> contexts);
}