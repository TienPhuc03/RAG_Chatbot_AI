package com.ragchatbot.application.dto.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EvaluationRequest(
        @NotBlank(message = "Question must not be blank")
        @Size(max = 1000, message = "Question must be at most 1000 characters")
        String question,

        @NotBlank(message = "Ground truth must not be blank")
        @Size(max = 5000, message = "Ground truth must be at most 5000 characters")
        String groundTruth,

        @NotBlank(message = "Answer must not be blank")
        @Size(max = 5000, message = "Answer must be at most 5000 characters")
        String answer,

        List<String> contexts,

        String experimentType,

        String chunkingStrategy,

        String embeddingModel
) {
}
