import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";

import {
  fetchBenchmarkJobStatus,
  fetchBenchmarkResults,
  fetchTestSetSummary,
  runBenchmark,
} from "../services/benchmarkService";

const BenchmarkContext =
  createContext(null);

function wait(milliseconds) {
  return new Promise((resolve) => {
    setTimeout(resolve, milliseconds);
  });
}

export function BenchmarkProvider({
  children,
}) {
  /*
   * Cấu hình mặc định là winner
   * của RQ1 và RQ2.
   */
  const [strategy, setStrategy] =
    useState("HIERARCHICAL");

  const [
    embeddingModel,
    setEmbeddingModel,
  ] = useState("BGE_M3");

  const [
    experimentType,
    setExperimentType,
  ] = useState("RAG");

  const [
    benchmarkMode,
    setBenchmarkMode,
  ] = useState("FULL_PIPELINE");

  const [runId, setRunId] =
    useState("");

  const [topK, setTopK] =
    useState(5);

  /*
   * Run ID dùng cho ô tìm kiếm.
   */
  const [
    filterRunId,
    setFilterRunId,
  ] = useState("");

  /*
   * Run ID hiện đang được hiển thị
   * trong bảng kết quả.
   */
  const [
    activeResultsRunId,
    setActiveResultsRunId,
  ] = useState("");

  const [
    testSetSummary,
    setTestSetSummary,
  ] = useState(null);

  const [jobStatus, setJobStatus] =
    useState(null);

  const [results, setResults] =
    useState([]);

  const [loading, setLoading] =
    useState(false);

  const [message, setMessage] =
    useState("");

  /**
   * Tải kết quả benchmark.
   */
  const loadResults = useCallback(
    async (targetRunId = "") => {
      const normalizedRunId =
        targetRunId.trim();

      try {
        const data =
          await fetchBenchmarkResults(
            normalizedRunId
          );

        setResults(
          Array.isArray(data)
            ? data
            : []
        );

        setActiveResultsRunId(
          normalizedRunId
        );
      } catch (error) {
        setResults([]);

        setMessage(
          error.message
            || "Không tải được kết quả benchmark."
        );
      }
    },
    []
  );

  /**
   * Đọc thông tin test set.
   */
  const loadTestSetSummary =
    useCallback(async () => {
      try {
        const data =
          await fetchTestSetSummary();

        setTestSetSummary(data);
      } catch (error) {
        setTestSetSummary(null);

        setMessage(
          error.message
            || "Không kiểm tra được test set."
        );
      }
    }, []);

  /**
   * Khi mở trang, tải bảng tổng hợp
   * và thông tin test set.
   */
  useEffect(() => {
    loadResults("");
    loadTestSetSummary();
  }, [
    loadResults,
    loadTestSetSummary,
  ]);

  /**
   * Chạy benchmark và polling job status.
   */
  const runBenchmarkJob =
    useCallback(async () => {
      if (loading) {
        return;
      }

      const normalizedRunId =
        runId.trim();

      const normalizedTopK =
        Number(topK);

      if (!normalizedRunId) {
        setMessage(
          "Run ID không được để trống."
        );

        return;
      }

      if (
        !Number.isInteger(
          normalizedTopK
        )
        || normalizedTopK < 1
        || normalizedTopK > 20
      ) {
        setMessage(
          "Top K phải là số nguyên từ 1 đến 20."
        );

        return;
      }

      setLoading(true);
      setMessage("");
      setJobStatus(null);

      try {
        const response =
          await runBenchmark({
            strategy,
            embeddingModel,
            experimentType,
            benchmarkMode,
            runId: normalizedRunId,
            topK: normalizedTopK,
          });

        let currentStatus =
          await fetchBenchmarkJobStatus(
            response.jobId
          );

        setJobStatus(
          currentStatus
        );

        /*
         * Polling mỗi 2 giây.
         */
        while (
          currentStatus.status
            === "PENDING"
          || currentStatus.status
            === "RUNNING"
        ) {
          await wait(2000);

          currentStatus =
            await fetchBenchmarkJobStatus(
              response.jobId
            );

          setJobStatus(
            currentStatus
          );
        }

        /*
         * Khi job kết thúc, tự động
         * hiển thị kết quả của run vừa chạy.
         */
        setFilterRunId(
          normalizedRunId
        );

        await loadResults(
          normalizedRunId
        );

        if (
          currentStatus.status
            === "FAILED"
        ) {
          setMessage(
            currentStatus.message
              || "Benchmark thất bại."
          );
        } else {
          setMessage(
            `Benchmark kết thúc với trạng thái: ${currentStatus.status}`
          );
        }
      } catch (error) {
        setMessage(
          error.message
            || "Benchmark thất bại."
        );
      } finally {
        setLoading(false);
      }
    }, [
      benchmarkMode,
      embeddingModel,
      experimentType,
      loadResults,
      loading,
      runId,
      strategy,
      topK,
    ]);

  return (
    <BenchmarkContext.Provider
      value={{
        strategy,
        setStrategy,

        embeddingModel,
        setEmbeddingModel,

        experimentType,
        setExperimentType,

        benchmarkMode,
        setBenchmarkMode,

        runId,
        setRunId,

        topK,
        setTopK,

        filterRunId,
        setFilterRunId,

        activeResultsRunId,

        testSetSummary,

        jobStatus,
        results,
        loading,
        message,

        loadResults,
        loadTestSetSummary,
        runBenchmarkJob,
      }}
    >
      {children}
    </BenchmarkContext.Provider>
  );
}

export function useBenchmark() {
  const context =
    useContext(BenchmarkContext);

  if (!context) {
    throw new Error(
      "useBenchmark must be used within BenchmarkProvider"
    );
  }

  return context;
}