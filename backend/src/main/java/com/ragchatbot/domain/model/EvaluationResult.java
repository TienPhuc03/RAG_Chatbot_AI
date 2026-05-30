package com.ragchatbot.domain.model;

/**
 * Chứa kết quả đánh giá (chấm điểm) cho một câu trả lời của chatbot.
 *
 * @param exactMatch Điểm khớp chính xác hoàn toàn (0.0 hoặc 1.0) so với đáp án chuẩn.
 * @param f1 Điểm F1 đo lường tỷ lệ trùng lặp từ vựng.
 * @param faithfulness Điểm trung thực (RAGAS).
 * @param answerRelevancy Điểm bám sát (RAGAS).
 * @param contextPrecision Điểm chính xác ngữ cảnh (RAGAS).
 * @param contextRecall Điểm bao phủ ngữ cảnh (RAGAS).
 */
public record EvaluationResult(
        double exactMatch,
        double f1,
        double faithfulness,
        double answerRelevancy,
        double contextPrecision,
        double contextRecall
) {}