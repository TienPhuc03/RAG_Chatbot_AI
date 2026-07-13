import request from "./apiClient";

/**
 * Tạo benchmark job.
 */
export function runBenchmark(payload) {
  return request("/api/benchmark/run", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * Lấy trạng thái benchmark job.
 */
export function fetchBenchmarkJobStatus(jobId) {
  const encodedJobId =
    encodeURIComponent(jobId);

  return request(
    `/api/benchmark/jobs/${encodedJobId}/status`
  );
}

/**
 * Lấy kết quả benchmark.
 *
 * - Có runId: chỉ lấy kết quả của run đó.
 * - Không có runId: lấy bảng tổng hợp.
 */
export function fetchBenchmarkResults(
  runId = ""
) {
  const normalizedRunId =
    runId.trim();

  const queryString =
    normalizedRunId.length > 0
      ? `?runId=${encodeURIComponent(
          normalizedRunId
        )}`
      : "";

  return request(
    `/api/benchmark/results${queryString}`
  );
}

/**
 * Kiểm tra số lượng câu trong test set.
 */
export function fetchTestSetSummary() {
  return request(
    "/api/benchmark/test-set/summary"
  );
}