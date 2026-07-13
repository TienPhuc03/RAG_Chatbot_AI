package com.ragchatbot.api;

import com.ragchatbot.application.dto.benchmark.BenchmarkSummaryDto;
import com.ragchatbot.domain.enums.BenchmarkMode;
import com.ragchatbot.domain.model.TestCase;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/benchmark")
@Tag(
        name = "Benchmark",
        description = "API chay benchmark va xem ket qua tong hop"
)
public class BenchmarkController {

    private static final int EXPECTED_TEST_CASE_COUNT = 50;
    private static final int DEFAULT_TOP_K = 5;

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

    /**
     * Kiểm tra test set.
     *
     * TestSetLoader sẽ thực hiện validation các trường bắt buộc
     * trước khi trả danh sách test case.
     */
    @Operation(
            summary = "Lay tong quan test set",
            description = "Tra ve so luong cau hoi va canh bao neu khong du 50 cau"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Doc test set thanh cong"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Test set khong hop le hoac khong doc duoc"
            )
    })
    @GetMapping("/test-set/summary")
    public ResponseEntity<Map<String, Object>> getTestSetSummary() {
        try {
            List<TestCase> testCases =
                    testSetLoader.loadTestCases();

            int totalQuestions =
                    testCases.size();

            long outOfScopeQuestions =
                    testCases.stream()
                            .filter(testCase ->
                                    Boolean.TRUE.equals(
                                            testCase.outOfScope()
                                    )
                            )
                            .count();

            long inScopeQuestions =
                    totalQuestions - outOfScopeQuestions;

            boolean expectedQuestionCount =
                    totalQuestions == EXPECTED_TEST_CASE_COUNT;

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "totalQuestions",
                    totalQuestions
            );

            response.put(
                    "inScopeQuestions",
                    inScopeQuestions
            );

            response.put(
                    "outOfScopeQuestions",
                    outOfScopeQuestions
            );

            response.put(
                    "expectedQuestions",
                    EXPECTED_TEST_CASE_COUNT
            );

            response.put(
                    "expectedQuestionCount",
                    expectedQuestionCount
            );

            response.put(
                    "status",
                    expectedQuestionCount
                            ? "SUCCESS"
                            : "WARNING"
            );

            response.put(
                    "warning",
                    expectedQuestionCount
                            ? ""
                            : "Test set hien co "
                            + totalQuestions
                            + " cau, khong du "
                            + EXPECTED_TEST_CASE_COUNT
                            + " cau theo thiet ke."
            );

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("status", "ERROR");

            response.put(
                    "message",
                    exception.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    /**
     * Tạo benchmark job bất đồng bộ.
     */
    @Operation(summary = "Chay benchmark")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Tao benchmark job thanh cong"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request khong hop le"
            )
    })
    @PostMapping("/run")
    public ResponseEntity<RunBenchmarkResponse> runBenchmark(
            @Valid @RequestBody RunBenchmarkRequest request
    ) {
        String normalizedRunId =
                request.runId().trim();

        int resolvedTopK =
                request.topK() == null
                        ? DEFAULT_TOP_K
                        : request.topK();

        BenchmarkMode benchmarkMode =
                BenchmarkMode.valueOf(
                        request.benchmarkMode()
                );

        String jobId =
                UUID.randomUUID().toString();

        String description =
                "runId=" + normalizedRunId
                        + " | strategy=" + request.strategy()
                        + " | embedding=" + request.embeddingModel()
                        + " | experiment=" + request.experimentType()
                        + " | mode=" + benchmarkMode.name()
                        + " | topK=" + resolvedTopK;

        int totalCases =
                testSetLoader.loadTestCases().size();

        jobRegistry.register(
                jobId,
                description,
                totalCases
        );

        benchmarkRunnerService.runBenchmark(
                jobId,
                new BenchmarkConfig(
                        request.strategy(),
                        request.embeddingModel(),
                        request.experimentType(),
                        benchmarkMode,
                        normalizedRunId,
                        resolvedTopK
                )
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        new RunBenchmarkResponse(
                                jobId,
                                BenchmarkJobStatus.PENDING,
                                description,
                                Instant.now()
                        )
                );
    }

    /**
     * Lấy trạng thái benchmark job.
     */
    @Operation(summary = "Lay trang thai benchmark job")
    @GetMapping("/jobs/{jobId}/status")
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = "Benchmark job ID")
            @PathVariable String jobId
    ) {
        JobSnapshot snapshot =
                jobRegistry.get(jobId);

        if (snapshot == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Benchmark job khong ton tai: "
                            + jobId
            );
        }

        return ResponseEntity.ok(
                new JobStatusResponse(
                        snapshot.jobId(),
                        snapshot.status(),
                        snapshot.message(),
                        snapshot.startedAt(),
                        snapshot.completedAt(),
                        snapshot.totalCases(),
                        snapshot.doneCases()
                )
        );
    }

    /**
     * Lấy kết quả tổng hợp.
     *
     * Khi truyền runId, API chỉ lấy dữ liệu của run đó.
     * Khi không truyền runId, API trả bảng tổng hợp hiện tại.
     */
    @Operation(summary = "Lay ket qua tong hop benchmark")
    @GetMapping("/results")
    public ResponseEntity<List<BenchmarkSummaryDto>>
    getAggregateResults(
            @RequestParam(required = false)
            String runId
    ) {
        if (runId == null || runId.isBlank()) {
            return ResponseEntity.ok(
                    benchmarkResultRepository
                            .findAverageMetricsByStrategyAndModel()
            );
        }

        return ResponseEntity.ok(
                benchmarkResultRepository
                        .findFinalMetricsByRunId(
                                runId.trim()
                        )
        );
    }

    /**
     * Request chạy benchmark.
     *
     * topK có thể null để giữ tương thích với request cũ.
     * Khi null, controller dùng mặc định topK = 5.
     */
    public record RunBenchmarkRequest(

            @NotBlank(
                    message = "strategy khong duoc de trong"
            )
            @Pattern(
                    regexp =
                            "FIXED_SIZE|SEMANTIC|HIERARCHICAL",
                    message = "strategy khong hop le"
            )
            String strategy,

            @NotBlank(
                    message =
                            "embeddingModel khong duoc de trong"
            )
            @Pattern(
                    regexp =
                            "MULTILINGUAL_E5_BASE"
                                    + "|PHOBERT_BASE"
                                    + "|BGE_M3"
                                    + "|GEMINI_EMBEDDING_001",
                    message =
                            "embeddingModel khong hop le"
            )
            String embeddingModel,

            @NotBlank(
                    message =
                            "experimentType khong duoc de trong"
            )
            @Pattern(
                    regexp = "RAG|FINETUNE",
                    message =
                            "experimentType khong hop le"
            )
            String experimentType,

            @NotBlank(
                    message =
                            "benchmarkMode khong duoc de trong"
            )
            @Pattern(
                    regexp =
                            "RETRIEVAL_ONLY"
                                    + "|FULL_PIPELINE"
                                    + "|EVALUATION_ONLY",
                    message =
                            "benchmarkMode khong hop le"
            )
            String benchmarkMode,

            @NotBlank(
                    message = "runId khong duoc de trong"
            )
            String runId,

            @Min(
                    value = 1,
                    message =
                            "topK phai lon hon hoac bang 1"
            )
            @Max(
                    value = 20,
                    message =
                            "topK khong duoc lon hon 20"
            )
            Integer topK
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