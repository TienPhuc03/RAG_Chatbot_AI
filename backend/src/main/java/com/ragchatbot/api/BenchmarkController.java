package com.ragchatbot.api;

import com.ragchatbot.application.dto.benchmark.BenchmarkSummaryDto;
import com.ragchatbot.infrastructure.benchmark.BenchmarkJobRegistry;
import com.ragchatbot.infrastructure.benchmark.BenchmarkJobRegistry.JobSnapshot;
import com.ragchatbot.infrastructure.benchmark.BenchmarkJobStatus;
import com.ragchatbot.infrastructure.benchmark.BenchmarkRunnerService;
import com.ragchatbot.infrastructure.benchmark.BenchmarkRunnerService.BenchmarkConfig;
import com.ragchatbot.infrastructure.benchmark.TestSetLoader;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/benchmark")
@Tag(name = "Benchmark", description = "API chay benchmark va xem ket qua tong hop")
public class BenchmarkController {

    private final BenchmarkRunnerService benchmarkRunnerService;
    private final BenchmarkJobRegistry jobRegistry;
    private final BenchmarkResultRepository benchmarkResultRepository;
    private final TestSetLoader testSetLoader;

    public BenchmarkController(
            BenchmarkRunnerService benchmarkRunnerService,
            BenchmarkJobRegistry jobRegistry,
            BenchmarkResultRepository benchmarkResultRepository,
            TestSetLoader testSetLoader
    ) {
        this.benchmarkRunnerService = benchmarkRunnerService;
        this.jobRegistry = jobRegistry;
        this.benchmarkResultRepository = benchmarkResultRepository;
        this.testSetLoader = testSetLoader;
    }

    @Operation(summary = "Chay benchmark")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Tao job thanh cong"),
            @ApiResponse(responseCode = "400", description = "Tham so khong hop le")
    })
    @PostMapping("/run")
    public ResponseEntity<RunBenchmarkResponse> runBenchmark(
            @Valid @RequestBody RunBenchmarkRequest request
    ) {
        String jobId = UUID.randomUUID().toString();
        String description = "strategy=" + request.strategy()
                + " | embedding=" + request.embeddingModel()
                + " | experiment=" + request.experimentType();

        int totalCases = testSetLoader.loadTestCases().size();
        jobRegistry.register(jobId, description, totalCases);

        benchmarkRunnerService.runBenchmark(
                jobId,
                new BenchmarkConfig(
                        request.strategy(),
                        request.embeddingModel(),
                        request.experimentType()
                )
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new RunBenchmarkResponse(jobId, BenchmarkJobStatus.PENDING, description, Instant.now()));
    }

    @Operation(summary = "Lay trang thai benchmark job")
    @GetMapping("/jobs/{jobId}/status")
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = "Job id benchmark")
            @PathVariable String jobId
    ) {
        JobSnapshot snapshot = jobRegistry.get(jobId);
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Benchmark job khong ton tai: " + jobId);
        }

        return ResponseEntity.ok(new JobStatusResponse(
                snapshot.jobId(),
                snapshot.status(),
                snapshot.message(),
                snapshot.startedAt(),
                snapshot.completedAt(),
                snapshot.totalCases(),
                snapshot.doneCases()
        ));
    }

    @Operation(summary = "Lay ket qua benchmark tong hop")
    @GetMapping("/results")
    public ResponseEntity<List<BenchmarkSummaryDto>> getAggregateResults() {
        return ResponseEntity.ok(benchmarkResultRepository.findAverageMetricsByStrategyAndModel());
    }

    public record RunBenchmarkRequest(
            @NotBlank(message = "strategy khong duoc de trong")
            @Pattern(
                    regexp = "FIXED_SIZE|SEMANTIC|HIERARCHICAL",
                    message = "strategy phai la FIXED_SIZE, SEMANTIC hoac HIERARCHICAL"
            )
            String strategy,

            @NotBlank(message = "embeddingModel khong duoc de trong")
            @Pattern(
                    regexp = "MULTILINGUAL_E5_BASE|PHOBERT_BASE|BGE_M3|GEMINI_EMBEDDING_001",
                    message = "embeddingModel phai la GEMINI_EMBEDDING_001, MULTILINGUAL_E5_BASE, PHOBERT_BASE hoac BGE_M3"
            )
            String embeddingModel,

            @NotBlank(message = "experimentType khong duoc de trong")
            @Pattern(
                    regexp = "RAG|FINETUNE",
                    message = "experimentType phai la RAG hoac FINETUNE"
            )
            String experimentType
    ) {
    }

    public record RunBenchmarkResponse(
            String jobId,
            BenchmarkJobStatus status,
            String description,
            Instant createdAt
    ) {
    }

    public record JobStatusResponse(
            String jobId,
            BenchmarkJobStatus status,
            String message,
            Instant startedAt,
            Instant completedAt,
            int totalCases,
            int doneCases
    ) {
    }
}
