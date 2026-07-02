import { useEffect, useState } from "react";
import SectionIntro from "../components/SectionIntro";
import {
  fetchBenchmarkJobStatus,
  fetchBenchmarkResults,
  runBenchmark,
} from "../services/benchmarkService";

function BenchmarkPage() {
  const [strategy, setStrategy] = useState("SEMANTIC");
  const [embeddingModel, setEmbeddingModel] = useState("GEMINI_EMBEDDING_001");
  const [experimentType, setExperimentType] = useState("RAG");
  const [jobStatus, setJobStatus] = useState(null);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  async function loadResults() {
    try {
      const data = await fetchBenchmarkResults();
      setResults(data);
    } catch (error) {
      setMessage(error.message || "Khong tai duoc ket qua benchmark.");
    }
  }

  useEffect(() => {
    loadResults();
  }, []);

  async function handleRunBenchmark(event) {
    event.preventDefault();
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
        await new Promise((resolve) => setTimeout(resolve, 2000));
        current = await fetchBenchmarkJobStatus(response.jobId);
        setJobStatus(current);
      }

      await loadResults();
      if (current.status === "FAILED") {
        setMessage(current.message || "Benchmark that bai.");
      } else {
        setMessage(`Benchmark ket thuc voi trang thai: ${current.status}`);
      }
    } catch (error) {
      setMessage(error.message || "Benchmark that bai.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <SectionIntro
        eyebrow="Research Flow"
        title="Benchmark"
        description="Chay benchmark tu giao dien, theo doi tien do va xem bang ket qua tong hop ngay trong workspace."
      />

      <form
        onSubmit={handleRunBenchmark}
        className="mt-8 grid gap-4 rounded-[2rem] border border-border-subtle bg-white p-6 shadow-soft lg:grid-cols-3"
      >
        <label className="text-sm text-text-secondary">
          Strategy
          <select
            value={strategy}
            onChange={(event) => setStrategy(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          >
            <option value="FIXED_SIZE">FIXED_SIZE</option>
            <option value="HIERARCHICAL">HIERARCHICAL</option>
            <option value="SEMANTIC">SEMANTIC</option>
          </select>
        </label>
        <label className="text-sm text-text-secondary">
          Embedding model
          <select
            value={embeddingModel}
            onChange={(event) => setEmbeddingModel(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          >
            <option value="GEMINI_EMBEDDING_001">GEMINI_EMBEDDING_001</option>
            <option value="MULTILINGUAL_E5_BASE">MULTILINGUAL_E5_BASE</option>
            <option value="PHOBERT_BASE">PHOBERT_BASE</option>
            <option value="BGE_M3">BGE_M3</option>
          </select>
        </label>
        <label className="text-sm text-text-secondary">
          Experiment type
          <select
            value={experimentType}
            onChange={(event) => setExperimentType(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          >
            <option value="RAG">RAG</option>
            <option value="FINETUNE">FINETUNE</option>
          </select>
        </label>
        <div className="lg:col-span-3">
          <button
            type="submit"
            disabled={loading}
            className="h-12 rounded-2xl bg-teal px-5 text-sm font-semibold text-white transition hover:bg-teal-strong disabled:bg-slate-300"
          >
            {loading ? "Dang chay benchmark..." : "Chay benchmark"}
          </button>
        </div>
      </form>

      {jobStatus ? (
        <div className="mt-4 rounded-2xl border border-border-subtle bg-white px-5 py-4 text-sm text-text-secondary shadow-soft">
          Job: {jobStatus.jobId} | Status: {jobStatus.status} | Done: {jobStatus.doneCases}/{jobStatus.totalCases}
          {jobStatus.message ? ` | Message: ${jobStatus.message}` : ""}
        </div>
      ) : null}

      {message ? (
        <div className="mt-4 rounded-2xl border border-border-subtle bg-white px-5 py-4 text-sm text-text-secondary shadow-soft">
          {message}
        </div>
      ) : null}

      <div className="mt-8 overflow-hidden rounded-[2rem] border border-border-subtle bg-white shadow-soft">
        <div className="border-b border-border-subtle px-6 py-4 text-lg font-semibold text-text-primary">
          Ket qua tong hop
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-surface-soft text-text-secondary">
              <tr>
                <th className="px-4 py-3">Strategy</th>
                <th className="px-4 py-3">Embedding</th>
                <th className="px-4 py-3">Type</th>
                <th className="px-4 py-3">Runs</th>
                <th className="px-4 py-3">F1</th>
                <th className="px-4 py-3">Faithfulness</th>
                <th className="px-4 py-3">Latency</th>
              </tr>
            </thead>
            <tbody>
              {results.map((item, index) => (
                <tr
                  key={`${item.chunkingStrategy}-${item.embeddingModel}-${index}`}
                  className="border-t border-border-subtle"
                >
                  <td className="px-4 py-3">{item.chunkingStrategy}</td>
                  <td className="px-4 py-3">{item.embeddingModel}</td>
                  <td className="px-4 py-3">{item.experimentType}</td>
                  <td className="px-4 py-3">{item.runCount}</td>
                  <td className="px-4 py-3">{item.avgF1Score?.toFixed?.(3) ?? item.avgF1Score}</td>
                  <td className="px-4 py-3">
                    {item.avgFaithfulness?.toFixed?.(3) ?? item.avgFaithfulness}
                  </td>
                  <td className="px-4 py-3">{item.avgLatencyMs?.toFixed?.(1) ?? item.avgLatencyMs}</td>
                </tr>
              ))}
              {results.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-4 py-4 text-text-secondary">
                    Chua co ket qua benchmark.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default BenchmarkPage;
