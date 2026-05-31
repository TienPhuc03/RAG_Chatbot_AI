package com.ragchatbot.domain.model;

/**
 * Chứa kết quả đánh giá  cho một câu trả lời của chatbot dựa trên nhiều tiêu chí đo lường.
 *
 * @param exactMatch Điểm khớp chính xác hoàn toàn (0.0 hoặc 1.0) so với đáp án chuẩn.
 * @param f1 Điểm F1 đo lường tỷ lệ trùng lặp từ vựng giữa câu trả lời sinh ra và đáp án chuẩn.
 * @param faithfulness Điểm trung thực (RAGAS): đo lường xem câu trả lời có được suy ra hoàn toàn từ tài liệu không (phát hiện ảo giác).
 * @param answerRelevancy Điểm bám sát (RAGAS): đo lường xem câu trả lời có đi thẳng vào trọng tâm câu hỏi hay không.
 * @param contextPrecision Điểm chính xác ngữ cảnh (RAGAS): đo lường khả năng đưa tài liệu hữu ích nhất lên top đầu kết quả tìm kiếm.
 * @param contextRecall Điểm bao phủ ngữ cảnh (RAGAS): đo lường xem các tài liệu tìm được có đủ thông tin để trả lời trọn vẹn câu hỏi không.
 */
public record EvaluationResult(
        double exactMatch,
        double f1,
        double faithfulness,
        double answerRelevancy,
        double contextPrecision,
        double contextRecall
) {}