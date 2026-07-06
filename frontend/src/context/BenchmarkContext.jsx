import { createContext, useCallback, useContext, useEffect, useState } from "react";
import {
  fetchBenchmarkJobStatus,
  fetchBenchmarkResults,
  runBenchmark,
} from "../services/benchmarkService";

const BenchmarkContext = createContext(null);

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function BenchmarkProvider({ children }) {
  const [strategy, setStrategy] = useState("SEMANTIC");
  const [embeddingModel, setEmbeddingModel] = useState("GEMINI_EMBEDDING_001");
  const [experimentType, setExperimentType] = useState("RAG");
  const [jobStatus, setJobStatus] = useState(null);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const loadResults = useCallback(async () => {
    try {
      const data = await fetchBenchmarkResults();
      setResults(data);
    } catch (error) {
      setMessage(error.message || "Không tải được kết quả benchmark.");
    }
  }, []);

  useEffect(() => {
    loadResults();
  }, [loadResults]);

  const runBenchmarkJob = useCallback(async () => {
    if (loading) {
      return;
    }

    setLoading(true);
    setMessage("");

    try {
      const response = await runBenchmark({
        strategy,
        embeddingModel,
        experimentType,
      });

      let current = await fetchBenchmarkJobStatus(response.jobId);
      setJobStatus(current);

      while (current.status === "PENDING" || current.status === "RUNNING") {
        await wait(2000);
        current = await fetchBenchmarkJobStatus(response.jobId);
        setJobStatus(current);
      }

      await loadResults();
      if (current.status === "FAILED") {
        setMessage(current.message || "Benchmark thất bại.");
      } else {
        setMessage(`Benchmark kết thúc với trạng thái: ${current.status}`);
      }
    } catch (error) {
      setMessage(error.message || "Benchmark thất bại.");
    } finally {
      setLoading(false);
    }
  }, [embeddingModel, experimentType, loadResults, loading, strategy]);

  return (
    <BenchmarkContext.Provider
      value={{
        strategy,
        setStrategy,
        embeddingModel,
        setEmbeddingModel,
        experimentType,
        setExperimentType,
        jobStatus,
        results,
        loading,
        message,
        loadResults,
        runBenchmarkJob,
      }}
    >
      {children}
    </BenchmarkContext.Provider>
  );
}

export function useBenchmark() {
  const context = useContext(BenchmarkContext);

  if (!context) {
    throw new Error("useBenchmark must be used within BenchmarkProvider");
  }

  return context;
}
