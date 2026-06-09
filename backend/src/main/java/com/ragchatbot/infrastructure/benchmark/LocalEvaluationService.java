package com.ragchatbot.infrastructure.benchmark;

import com.ragchatbot.domain.model.EvaluationResult;
import com.ragchatbot.domain.port.EvaluationService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LocalEvaluationService implements EvaluationService {

    @Override
    public EvaluationResult evaluate(String question, String groundTruth, String answer, List<String> contexts) {
        double exactMatch = calculateExactMatch(groundTruth, answer);
        double f1Score = calculateF1Score(groundTruth, answer);

        // Bốn chỉ số RAGAS tạm thời gán 0.0, sẽ tích hợp ở task W4-15
        return new EvaluationResult(exactMatch, f1Score, 0.0, 0.0, 0.0, 0.0);
    }

    private double calculateExactMatch(String groundTruth, String answer) {
        if (groundTruth == null || answer == null) {
            return 0.0;
        }
        String normalizedTruth = normalizeText(groundTruth);
        String normalizedAnswer = normalizeText(answer);

        return normalizedTruth.equals(normalizedAnswer) ? 1.0 : 0.0;
    }

    private double calculateF1Score(String groundTruth, String answer) {
        if (groundTruth == null || answer == null || groundTruth.isEmpty() || answer.isEmpty()) {
            return 0.0;
        }

        List<String> truthTokens = Arrays.asList(normalizeText(groundTruth).split("\\s+"));
        List<String> answerTokens = Arrays.asList(normalizeText(answer).split("\\s+"));

        if (truthTokens.isEmpty() || answerTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> commonTokens = new HashSet<>(answerTokens);
        commonTokens.retainAll(truthTokens);

        int numCommon = commonTokens.size();
        if (numCommon == 0) {
            return 0.0;
        }

        double precision = (double) numCommon / answerTokens.size();
        double recall = (double) numCommon / truthTokens.size();

        return (2 * precision * recall) / (precision + recall);
    }

    /**
     * Chuẩn hóa chuỗi: chuyển thành chữ thường và loại bỏ tất cả dấu câu, giữ lại tiếng Việt.
     */
    private String normalizeText(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ\\s]", "").trim();
    }
}