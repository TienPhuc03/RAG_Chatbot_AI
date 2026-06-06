package com.ragchatbot.domain.port;

import com.ragchatbot.domain.model.EvaluationResult;
import java.util.List;

public interface EvaluationService {
    EvaluationResult evaluate(String question, String groundTruth, String answer, List<String> contexts);
}