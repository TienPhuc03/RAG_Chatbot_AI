import SectionIntro from "../components/SectionIntro";
import { useBenchmark } from "../context/BenchmarkContext";

function BenchmarkPage() {
  const {
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
    runBenchmarkJob,
  } = useBenchmark();

  const hasFallbackResults = results.some((item) => (item.fallbackRunCount ?? 0) > 0);

  async function handleRunBenchmark(event) {
    event.preventDefault();
    await runBenchmarkJob();
  }

  return (
    <div className="scrollbar-subtle h-full overflow-y-auto px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto w-full max-w-7xl">
      <SectionIntro
        eyebrow="Quy trình nghiên cứu"
        title="Đánh giá benchmark"
        description="Chạy benchmark từ giao diện, theo dõi tiến độ và xem bảng kết quả tổng hợp ngay trong workspace."
      />

      <form
        onSubmit={handleRunBenchmark}
        className="mt-8 grid gap-4 rounded-[2rem] border border-border-subtle bg-white p-6 shadow-soft lg:grid-cols-3"
      >
        <label className="text-sm text-text-secondary">
          Chiến lược chunking
          <select
            value={strategy}
            onChange={(event) => setStrategy(event.target.value)}
            disabled={experimentType === "FINETUNE"}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          >
            <option value="FIXED_SIZE">FIXED_SIZE</option>
            <option value="HIERARCHICAL">HIERARCHICAL</option>
            <option value="SEMANTIC">SEMANTIC</option>
          </select>
        </label>
        <label className="text-sm text-text-secondary">
          Mô hình embedding
          <select
            value={embeddingModel}
            onChange={(event) => setEmbeddingModel(event.target.value)}
            disabled={experimentType === "FINETUNE"}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          >
            <option value="GEMINI_EMBEDDING_001">GEMINI_EMBEDDING_001</option>
            <option value="MULTILINGUAL_E5_BASE">MULTILINGUAL_E5_BASE</option>
            <option value="PHOBERT_BASE">PHOBERT_BASE</option>
            <option value="BGE_M3">BGE_M3</option>
          </select>
        </label>
        <label className="text-sm text-text-secondary">
          Loại thử nghiệm
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
            {loading ? "Đang chạy benchmark..." : "Chạy benchmark"}
          </button>
        </div>
      </form>

      {experimentType === "FINETUNE" ? (
        <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm text-amber-900">
          Benchmark FINETUNE đang bỏ qua lựa chọn strategy và embedding, kết quả sẽ được gom dưới nhãn N/A.
        </div>
      ) : null}

      {jobStatus ? (
        <div className="mt-4 rounded-2xl border border-border-subtle bg-white px-5 py-4 text-sm text-text-secondary shadow-soft">
          Job: {jobStatus.jobId} | Trạng thái: {jobStatus.status} | Đã xong: {jobStatus.doneCases}/{jobStatus.totalCases}
          {jobStatus.message ? ` | Thông báo: ${jobStatus.message}` : ""}
        </div>
      ) : null}

      {message ? (
        <div className="mt-4 rounded-2xl border border-border-subtle bg-white px-5 py-4 text-sm text-text-secondary shadow-soft">
          {message}
        </div>
      ) : null}

      {/* {hasFallbackResults ? (
        <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm text-amber-900">
          Có benchmark run đã fallback sang local evaluation thay vì ragas-service. Không nên dùng các run này làm kết luận chính trong báo cáo.
        </div>
      ) : null} */}

      <div className="mt-8 overflow-hidden rounded-[2rem] border border-border-subtle bg-white shadow-soft">
        <div className="border-b border-border-subtle px-6 py-4 text-lg font-semibold text-text-primary">
          Kết quả tổng hợp
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-surface-soft text-text-secondary">
              <tr>
                <th className="px-4 py-3">Chiến lược</th>
                <th className="px-4 py-3">Embedding</th>
                <th className="px-4 py-3">Loại</th>
                <th className="px-4 py-3">Số lần chạy</th>
                <th className="px-4 py-3">Nguồn đánh giá</th>
                <th className="px-4 py-3">Exact Match</th>
                <th className="px-4 py-3">F1</th>
                <th className="px-4 py-3">Faithfulness</th>
                <th className="px-4 py-3">Answer Rel.</th>
                <th className="px-4 py-3">Ctx Precision</th>
                <th className="px-4 py-3">Ctx Recall</th>
                <th className="px-4 py-3">Retrieval Hit</th>
                <th className="px-4 py-3">Độ trễ</th>
                <th className="px-4 py-3">Chi phí (USD ước tính)</th>
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
                  <td className="px-4 py-3">
                    {item.fallbackRunCount > 0
                      ? `${item.geminiJudgeRunCount} gemini, ${item.ollamaJudgeRunCount} ollama, ${item.fallbackRunCount} local fallback`
                      : `${item.geminiJudgeRunCount} gemini, ${item.ollamaJudgeRunCount} ollama`}
                  </td>
                  <td className="px-4 py-3">{item.avgExactMatch?.toFixed?.(3) ?? item.avgExactMatch}</td>
                  <td className="px-4 py-3">{item.avgF1Score?.toFixed?.(3) ?? item.avgF1Score}</td>
                  <td className="px-4 py-3">
                    {item.avgFaithfulness?.toFixed?.(3) ?? item.avgFaithfulness}
                  </td>
                  <td className="px-4 py-3">
                    {item.avgAnswerRelevancy?.toFixed?.(3) ?? item.avgAnswerRelevancy}
                  </td>
                  <td className="px-4 py-3">
                    {item.avgContextPrecision?.toFixed?.(3) ?? item.avgContextPrecision}
                  </td>
                  <td className="px-4 py-3">
                    {item.avgContextRecall?.toFixed?.(3) ?? item.avgContextRecall}
                  </td>
                  <td className="px-4 py-3">
                    {item.retrievalHitRate != null
                      ? `${(item.retrievalHitRate * 100).toFixed(1)}%`
                      : "-"}
                  </td>
                  <td className="px-4 py-3">{item.avgLatencyMs?.toFixed?.(1) ?? item.avgLatencyMs}</td>
                  <td className="px-4 py-3">{item.avgCostUsd?.toFixed?.(6) ?? item.avgCostUsd}</td>
                </tr>
              ))}
              {results.length === 0 ? (
                <tr>
                  <td colSpan="14" className="px-4 py-4 text-text-secondary">
                    Chưa có kết quả benchmark.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </div>
      </div>
    </div>
  );
}

export default BenchmarkPage;
