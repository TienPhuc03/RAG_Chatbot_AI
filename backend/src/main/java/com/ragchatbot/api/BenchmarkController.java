package com.ragchatbot.api;

import com.ragchatbot.application.dto.benchmark.BenchmarkSummaryDto;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * BenchmarkController — W4-13
 *
 * Cung cấp 3 endpoints:
 *   POST /api/benchmark/run              → Tạo job async, trả jobId ngay lập tức
 *   GET  /api/benchmark/jobs/{jobId}/status → Poll trạng thái job
 *   GET  /api/benchmark/results          → Tổng hợp aggregate từ DB
 */
@RestController
@RequestMapping("/api/benchmark")
@Tag(name = "Benchmark", description = "API chạy benchmark và xem kết quả tổng hợp")
public class BenchmarkController {

    private final BenchmarkRunnerService benchmarkRunnerService;
    private final BenchmarkJobRegistry jobRegistry;
    private final BenchmarkResultRepository benchmarkResultRepository;
    private final TestSetLoader testSetLoader;

    public BenchmarkController(BenchmarkRunnerService benchmarkRunnerService,
                                BenchmarkJobRegistry jobRegistry,
                                BenchmarkResultRepository benchmarkResultRepository,
                                TestSetLoader testSetLoader) {
        this.benchmarkRunnerService = benchmarkRunnerService;
        this.jobRegistry = jobRegistry;
        this.benchmarkResultRepository = benchmarkResultRepository;
        this.testSetLoader = testSetLoader;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/benchmark/run
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Khởi động một benchmark run bất đồng bộ.
     *
     * Controller tạo jobId ngay, đăng ký vào registry, rồi gọi @Async service.
     * Client nhận jobId trong response mà không cần chờ benchmark hoàn thành.
     */
    @Operation(
            summary = "Chạy benchmark",
            description = """
                    Khởi động một benchmark run với cấu hình chỉ định.
                    Trả về jobId ngay lập tức; client poll GET /jobs/{jobId}/status để theo dõi tiến trình.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Job đã được tạo, đang chạy async"),
            @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    })
    @PostMapping("/run")
    public ResponseEntity<RunBenchmarkResponse> runBenchmark(
            @Valid @RequestBody RunBenchmarkRequest request
    ) {
        String jobId = UUID.randomUUID().toString();
        String configDescription = "strategy=" + request.strategy()
                + " | embedding=" + request.embeddingModel()
                + " | experiment=" + request.experimentType();

        // Load test cases để biết tổng số lượng (dùng cho tiến trình poll)
        List<TestCase> testCases = testSetLoader.loadTestCases();

        // Đăng ký job vào registry (PENDING) trước khi gọi @Async để tránh race condition
        jobRegistry.register(jobId, configDescription, testCases.size());

        BenchmarkConfig config = new BenchmarkConfig(
            request.strategy(),
            request.embeddingModel(),
            request.experimentType(),
            request.collectionName()
        );

        // Gọi @Async — trả về ngay, benchmark chạy trên thread khác
        benchmarkRunnerService.runBenchmark(jobId, config);

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(new RunBenchmarkResponse(
                    jobId,
                    BenchmarkJobStatus.PENDING,
                    configDescription,
                    Instant.now()
            ));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/benchmark/jobs/{jobId}/status
    // ═══════════════════════════════════════════════════════════════════════

    @Operation(
            summary = "Lấy trạng thái benchmark job",
            description = "Poll trạng thái hiện tại của một job. Trả 404 nếu jobId không tồn tại."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trạng thái job"),
            @ApiResponse(responseCode = "404", description = "Job không tồn tại")
    })
    @GetMapping("/jobs/{jobId}/status")
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = "UUID của benchmark job", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String jobId
    ) {
        JobSnapshot snapshot = jobRegistry.get(jobId);
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Benchmark job không tồn tại: " + jobId);
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

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/benchmark/results
    // ═══════════════════════════════════════════════════════════════════════

    @Operation(
            summary = "Kết quả benchmark tổng hợp",
            description = """
                    Trả về danh sách aggregate metrics nhóm theo (chunkingStrategy × embeddingModel × experimentType).
                    Dùng cho Research Dashboard hiển thị bảng so sánh.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Danh sách kết quả tổng hợp")
    @GetMapping("/results")
    public ResponseEntity<List<BenchmarkSummaryDto>> getAggregateResults() {
        List<BenchmarkSummaryDto> summaries = benchmarkResultRepository.findAverageMetricsByStrategyAndModel();
        return ResponseEntity.ok(summaries);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Inner DTOs (request / response)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Request body cho POST /api/benchmark/run.
     *
     * @param strategy       Tên enum ChunkingStrategy (FIXED_SIZE | SEMANTIC | HIERARCHICAL)
     * @param embeddingModel Tên enum EmbeddingModel  (MULTILINGUAL_E5_BASE | TEXT_EMBEDDING_3_SMALL | ...)
     * @param experimentType Tên enum ExperimentType  (RAG_SYSTEM | FINE_TUNED_MODEL)
     * @param collectionName Tên Qdrant collection (có thể null, dùng collection mặc định)
     */
    public record RunBenchmarkRequest(
            @NotBlank(message = "strategy không được để trống")
            @Pattern(
                    regexp = "FIXED_SIZE|SEMANTIC|HIERARCHICAL",
                    message = "strategy phải là FIXED_SIZE, SEMANTIC hoặc HIERARCHICAL"
            )
            String strategy,

            @NotBlank(message = "embeddingModel không được để trống")
            @Pattern(
                    regexp = "MULTILINGUAL_E5_BASE|TEXT_EMBEDDING_3_SMALL|PHOBERT_BASE|BGE_M3|GEMINI_EMBEDDING_001",
                    message = "embeddingModel không hợp lệ"
            )
            String embeddingModel,

            @NotBlank(message = "experimentType không được để trống")
            @Pattern(
                    regexp = "RAG_SYSTEM|FINE_TUNED_MODEL",
                    message = "experimentType phải là RAG_SYSTEM hoặc FINE_TUNED_MODEL"
            )
            String experimentType,

            String collectionName
    ) {}

    /**
     * Response sau POST /api/benchmark/run — trả về ngay sau khi job được đăng ký.
     *
     * @param jobId       UUID để poll status
     * @param status      Trạng thái khởi tạo (luôn là PENDING)
     * @param description Mô tả cấu hình
     * @param createdAt   Thời điểm tạo job
     */
    public record RunBenchmarkResponse(
            String jobId,
            BenchmarkJobStatus status,
            String description,
            Instant createdAt
    ) {}

    /**
     * Response cho GET /api/benchmark/jobs/{jobId}/status.
     *
     * @param jobId       UUID của job
     * @param status      Trạng thái hiện tại (PENDING / RUNNING / COMPLETED / FAILED)
     * @param message     Mô tả hoặc thông báo lỗi nếu FAILED
     * @param startedAt   Thời điểm bắt đầu chạy
     * @param completedAt Thời điểm hoàn thành (null nếu chưa xong)
     * @param totalCases  Tổng số test case
     * @param doneCases   Số test case đã xong
     */
    public record JobStatusResponse(
            String jobId,
            BenchmarkJobStatus status,
            String message,
            Instant startedAt,
            Instant completedAt,
            int totalCases,
            int doneCases
    ) {}
}
