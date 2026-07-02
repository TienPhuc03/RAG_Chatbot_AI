import request from "./apiClient";

export function runBenchmark(payload) {
  return request("/api/benchmark/run", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function fetchBenchmarkJobStatus(jobId) {
  return request(`/api/benchmark/jobs/${jobId}/status`);
}

export function fetchBenchmarkResults() {
  return request("/api/benchmark/results");
}
