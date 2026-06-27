package com.ragchatbot.api;

import com.ragchatbot.application.dto.evaluation.EvaluationRequest;
import com.ragchatbot.application.dto.evaluation.EvaluationResponse;
import com.ragchatbot.application.usecase.evaluation.EvaluateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluate")
public class EvaluationController {

    private final EvaluateUseCase evaluateUseCase;

    public EvaluationController(EvaluateUseCase evaluateUseCase) {
        this.evaluateUseCase = evaluateUseCase;
    }

    @Operation(
            summary = "Evaluate RAG answer",
            description = "Evaluate generated answer with local metrics plus RAGAS metrics from the evaluation service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluation completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<EvaluationResponse> evaluate(@Valid @RequestBody EvaluationRequest request) {
        return ResponseEntity.ok(evaluateUseCase.execute(request));
    }
}
