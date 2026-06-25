package com.ragchatbot.infrastructure.benchmark;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry để theo dõi trạng thái các benchmark job.
 *
 * Vì benchmark chạy @Async, cần có nơi lưu trữ trạng thái mà controller
 * có thể poll được qua GET /api/benchmark/jobs/{jobId}/status.
 *
 * Thread-safe thông qua ConcurrentHashMap.
 */
@Component
public class BenchmarkJobRegistry {

    /**
     * Snapshot trạng thái của một job.
     *
     * @param jobId       UUID định danh job
     * @param status      Trạng thái hiện tại (PENDING / RUNNING / COMPLETED / FAILED)
     * @param message     Mô tả chi tiết (tên config, lỗi nếu có, ...)
     * @param startedAt   Thời điểm job bắt đầu chạy (null khi còn PENDING)
     * @param completedAt Thời điểm job kết thúc (null khi chưa xong)
     * @param totalCases  Tổng số test case của run này
     * @param doneCases   Số test case đã xử lý xong
     */
    public record JobSnapshot(
            String jobId,
            BenchmarkJobStatus status,
            String message,
            Instant startedAt,
            Instant completedAt,
            int totalCases,
            int doneCases
    ) {}

    private final ConcurrentHashMap<String, JobSnapshot> store = new ConcurrentHashMap<>();

    // ──────────────────────────── write helpers ────────────────────────────

    /** Tạo một job mới ở trạng thái PENDING. */
    public void register(String jobId, String configDescription, int totalCases) {
        store.put(jobId, new JobSnapshot(
                jobId,
                BenchmarkJobStatus.PENDING,
                configDescription,
                null,
                null,
                totalCases,
                0
        ));
    }

    /** Đánh dấu job bắt đầu chạy. */
    public void markRunning(String jobId) {
        update(jobId, BenchmarkJobStatus.RUNNING, null, Instant.now(), null, -1, -1);
    }

    /** Cập nhật tiến trình (doneCases tăng dần). */
    public void updateProgress(String jobId, int doneCases) {
        JobSnapshot old = store.get(jobId);
        if (old == null) return;
        store.put(jobId, new JobSnapshot(
                old.jobId(),
                old.status(),
                old.message(),
                old.startedAt(),
                null,
                old.totalCases(),
                doneCases
        ));
    }

    /** Đánh dấu job hoàn thành thành công. */
    public void markCompleted(String jobId) {
        update(jobId, BenchmarkJobStatus.COMPLETED, "Completed successfully", null, Instant.now(), -1, -1);
    }

    /** Đánh dấu job thất bại với thông điệp lỗi. */
    public void markFailed(String jobId, String errorMessage) {
        update(jobId, BenchmarkJobStatus.FAILED, errorMessage, null, Instant.now(), -1, -1);
    }

    // ──────────────────────────── read ────────────────────────────

    /** Lấy snapshot trạng thái của job, trả null nếu không tồn tại. */
    public JobSnapshot get(String jobId) {
        return store.get(jobId);
    }

    // ──────────────────────────── private ────────────────────────────

    private void update(String jobId,
                        BenchmarkJobStatus status,
                        String message,
                        Instant startedAt,
                        Instant completedAt,
                        int totalCases,
                        int doneCases) {
        store.compute(jobId, (k, old) -> {
            if (old == null) return null;
            return new JobSnapshot(
                    old.jobId(),
                    status,
                    message != null ? message : old.message(),
                    startedAt != null ? startedAt : old.startedAt(),
                    completedAt != null ? completedAt : old.completedAt(),
                    totalCases >= 0 ? totalCases : old.totalCases(),
                    doneCases >= 0 ? doneCases : old.doneCases()
            );
        });
    }
}
