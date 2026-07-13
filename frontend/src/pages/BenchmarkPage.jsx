import SectionIntro from "../components/SectionIntro";
import { useBenchmark } from "../context/BenchmarkContext";

/**
 * Hiển thị metric.
 *
 * null, undefined hoặc NaN đều hiển thị N/A.
 */
function formatMetric(
  value,
  digits = 3
) {
  if (
    value === null
    || value === undefined
    || Number.isNaN(Number(value))
  ) {
    return "N/A";
  }

  return Number(value).toFixed(
    digits
  );
}

/**
 * Hiển thị tỷ lệ dưới dạng phần trăm.
 */
function formatPercentage(value) {
  if (
    value === null
    || value === undefined
    || Number.isNaN(Number(value))
  ) {
    return "N/A";
  }

  return `${(
    Number(value) * 100
  ).toFixed(1)}%`;
}

function BenchmarkPage() {
  const {
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
  } = useBenchmark();

  async function handleRunBenchmark(
    event
  ) {
    event.preventDefault();

    await runBenchmarkJob();
  }

  async function handleSearchResults(
    event
  ) {
    event.preventDefault();

    await loadResults(
      filterRunId
    );
  }

  async function handleRefreshResults() {
    setFilterRunId("");

    await loadResults("");
    await loadTestSetSummary();
  }

  function handleExperimentTypeChange(
    event
  ) {
    const nextExperimentType =
      event.target.value;

    setExperimentType(
      nextExperimentType
    );

    /*
     * FINETUNE chỉ nên dùng FULL_PIPELINE.
     */
    if (
      nextExperimentType
        === "FINETUNE"
    ) {
      setBenchmarkMode(
        "FULL_PIPELINE"
      );
    }
  }

  const totalCases =
    jobStatus?.totalCases ?? 0;

  const doneCases =
    jobStatus?.doneCases ?? 0;

  const progressPercent =
    totalCases > 0
      ? Math.min(
          100,
          Math.round(
            doneCases
              / totalCases
              * 100
          )
        )
      : 0;

  const testSetHasWarning =
    testSetSummary
    && testSetSummary.totalQuestions
      !== 50;

  const displayedRunId =
    activeResultsRunId
    || "TỔNG HỢP";

  return (
    <div className="scrollbar-subtle h-full overflow-y-auto px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto w-full max-w-7xl">
        <SectionIntro
          eyebrow="Quy trình nghiên cứu"
          title="Đánh giá benchmark"
          description="Chạy benchmark, theo dõi tiến độ và xem kết quả theo Run ID."
        />

        {/* =============================
            FORM CHẠY BENCHMARK
        ============================== */}
        <form
          onSubmit={
            handleRunBenchmark
          }
          className="mt-8 grid gap-4 rounded-[2rem] border border-border-subtle bg-white p-6 shadow-soft md:grid-cols-2 lg:grid-cols-3"
        >
          <label className="text-sm text-text-secondary">
            Chiến lược chunking

            <select
              value={strategy}
              onChange={(event) =>
                setStrategy(
                  event.target.value
                )
              }
              disabled={
                experimentType
                  === "FINETUNE"
              }
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4 disabled:bg-slate-100"
            >
              <option value="FIXED_SIZE">
                FIXED_SIZE
              </option>

              <option value="SEMANTIC">
                SEMANTIC
              </option>

              <option value="HIERARCHICAL">
                HIERARCHICAL
              </option>
            </select>
          </label>

          <label className="text-sm text-text-secondary">
            Mô hình embedding

            <select
              value={
                embeddingModel
              }
              onChange={(event) =>
                setEmbeddingModel(
                  event.target.value
                )
              }
              disabled={
                experimentType
                  === "FINETUNE"
              }
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4 disabled:bg-slate-100"
            >
              <option value="BGE_M3">
                BGE_M3
              </option>

              <option value="MULTILINGUAL_E5_BASE">
                MULTILINGUAL_E5_BASE
              </option>

              <option value="PHOBERT_BASE">
                PHOBERT_BASE
              </option>

              <option value="GEMINI_EMBEDDING_001">
                GEMINI_EMBEDDING_001
              </option>
            </select>
          </label>

          <label className="text-sm text-text-secondary">
            Loại thử nghiệm

            <select
              value={
                experimentType
              }
              onChange={
                handleExperimentTypeChange
              }
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
            >
              <option value="RAG">
                RAG
              </option>

              <option value="FINETUNE">
                FINETUNE
              </option>
            </select>
          </label>

          <label className="text-sm text-text-secondary">
            Benchmark mode

            <select
              value={
                benchmarkMode
              }
              onChange={(event) =>
                setBenchmarkMode(
                  event.target.value
                )
              }
              disabled={
                experimentType
                  === "FINETUNE"
              }
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4 disabled:bg-slate-100"
            >
              <option value="RETRIEVAL_ONLY">
                RETRIEVAL_ONLY
              </option>

              <option value="FULL_PIPELINE">
                FULL_PIPELINE
              </option>
            </select>
          </label>

          <label className="text-sm text-text-secondary">
            Run ID

            <input
              type="text"
              value={runId}
              onChange={(event) =>
                setRunId(
                  event.target.value
                )
              }
              placeholder="MAIN_RAG_HIERARCHICAL_BGE_M3_V1"
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
            />
          </label>

          <label className="text-sm text-text-secondary">
            Top K

            <input
              type="number"
              min="1"
              max="20"
              value={topK}
              onChange={(event) =>
                setTopK(
                  event.target.value
                )
              }
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
            />
          </label>

          <div className="md:col-span-2 lg:col-span-3">
            <button
              type="submit"
              disabled={loading}
              className="h-12 rounded-2xl bg-teal px-5 text-sm font-semibold text-white transition hover:bg-teal-strong disabled:bg-slate-300"
            >
              {loading
                ? "Đang chạy benchmark..."
                : "Chạy benchmark"}
            </button>
          </div>
        </form>

        {/* =============================
            CẢNH BÁO
        ============================== */}
        {testSetHasWarning ? (
          <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-900">
            Test set hiện có{" "}
            {
              testSetSummary
                .totalQuestions
            }{" "}
            câu, không đủ 50 câu theo
            thiết kế.
          </div>
        ) : null}

        {testSetSummary
          && !testSetHasWarning ? (
          <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-900">
            Test set hợp lệ:{" "}
            {
              testSetSummary
                .totalQuestions
            }{" "}
            câu.
          </div>
        ) : null}

        <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm text-amber-900">
          Hãy kiểm tra tài liệu đã
          INDEXED đúng strategy và
          embedding model trước khi chạy
          benchmark.
        </div>

        {experimentType
          === "FINETUNE" ? (
          <div className="mt-4 rounded-2xl border border-blue-200 bg-blue-50 px-5 py-4 text-sm text-blue-900">
            FINETUNE sử dụng trực tiếp
            model Ollama fine-tuned và
            không thực hiện retrieval.
            Backend vẫn nhận strategy và
            embedding để tương thích với
            request hiện tại.
          </div>
        ) : null}

        {/* =============================
            TRẠNG THÁI JOB
        ============================== */}
        {jobStatus ? (
          <div className="mt-4 rounded-2xl border border-border-subtle bg-white px-5 py-4 text-sm text-text-secondary shadow-soft">
            <div className="grid gap-2 sm:grid-cols-2">
              <div>
                <strong>
                  Job ID:
                </strong>{" "}
                {jobStatus.jobId}
              </div>

              <div>
                <strong>
                  Trạng thái:
                </strong>{" "}
                {jobStatus.status}
              </div>

              <div>
                <strong>
                  Tiến độ:
                </strong>{" "}
                {doneCases}
                /
                {totalCases}
              </div>

              <div>
                <strong>
                  Thông báo:
                </strong>{" "}
                {jobStatus.message
                  || "N/A"}
              </div>
            </div>

            <div className="mt-4 h-3 overflow-hidden rounded-full bg-slate-200">
              <div
                className="h-full bg-teal transition-all duration-300"
                style={{
                  width:
                    `${progressPercent}%`,
                }}
              />
            </div>

            <div className="mt-2 text-xs text-text-secondary">
              {progressPercent}% hoàn thành
            </div>
          </div>
        ) : null}

        {message ? (
          <div className="mt-4 rounded-2xl border border-border-subtle bg-white px-5 py-4 text-sm text-text-secondary shadow-soft">
            {message}
          </div>
        ) : null}

        {/* =============================
            TÌM KẾT QUẢ THEO RUN ID
        ============================== */}
        <form
          onSubmit={
            handleSearchResults
          }
          className="mt-8 flex flex-col gap-3 rounded-[2rem] border border-border-subtle bg-white p-5 shadow-soft sm:flex-row"
        >
          <input
            type="text"
            value={filterRunId}
            onChange={(event) =>
              setFilterRunId(
                event.target.value
              )
            }
            placeholder="Nhập Run ID cần tìm"
            className="h-11 flex-1 rounded-2xl border border-border-subtle px-4"
          />

          <button
            type="submit"
            className="h-11 rounded-2xl bg-teal px-5 text-sm font-semibold text-white"
          >
            Tìm kết quả
          </button>

          <button
            type="button"
            onClick={
              handleRefreshResults
            }
            className="h-11 rounded-2xl border border-border-subtle px-5 text-sm font-semibold text-text-secondary"
          >
            Làm mới
          </button>
        </form>

        {/* =============================
            BẢNG KẾT QUẢ
        ============================== */}
        <div className="mt-8 overflow-hidden rounded-[2rem] border border-border-subtle bg-white shadow-soft">
          <div className="border-b border-border-subtle px-6 py-4">
            <div className="text-lg font-semibold text-text-primary">
              Kết quả tổng hợp
            </div>

            <div className="mt-1 text-sm text-text-secondary">
              Run ID đang hiển thị:{" "}
              <strong>
                {displayedRunId}
              </strong>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-surface-soft text-text-secondary">
                <tr>
                  <th className="px-4 py-3">
                    Run ID
                  </th>

                  <th className="px-4 py-3">
                    Chunking Strategy
                  </th>

                  <th className="px-4 py-3">
                    Embedding Model
                  </th>

                  <th className="px-4 py-3">
                    Experiment Type
                  </th>

                  <th className="px-4 py-3">
                    Run Count
                  </th>

                  <th className="px-4 py-3">
                    F1 Score
                  </th>

                  <th className="px-4 py-3">
                    Faithfulness
                  </th>

                  <th className="px-4 py-3">
                    Answer Relevancy
                  </th>

                  <th className="px-4 py-3">
                    Context Precision
                  </th>

                  <th className="px-4 py-3">
                    Context Recall
                  </th>

                  <th className="px-4 py-3">
                    Retrieval Hit Rate
                  </th>

                  <th className="px-4 py-3">
                    Latency (ms)
                  </th>
                </tr>
              </thead>

              <tbody>
                {results.map(
                  (item, index) => (
                    <tr
                      key={`${displayedRunId}-${item.chunkingStrategy}-${item.embeddingModel}-${item.experimentType}-${index}`}
                      className="border-t border-border-subtle"
                    >
                      <td className="px-4 py-3 font-medium">
                        {item.runId
                          || displayedRunId}
                      </td>

                      <td className="px-4 py-3">
                        {item.chunkingStrategy
                          || "N/A"}
                      </td>

                      <td className="px-4 py-3">
                        {item.embeddingModel
                          || "N/A"}
                      </td>

                      <td className="px-4 py-3">
                        {item.experimentType
                          || "N/A"}
                      </td>

                      <td className="px-4 py-3">
                        {item.runCount
                          ?? "N/A"}
                      </td>

                      <td className="px-4 py-3">
                        {formatMetric(
                          item.avgF1Score
                        )}
                      </td>

                      <td className="px-4 py-3">
                        {formatMetric(
                          item.avgFaithfulness
                        )}
                      </td>

                      <td className="px-4 py-3">
                        {formatMetric(
                          item.avgAnswerRelevancy
                        )}
                      </td>

                      <td className="px-4 py-3">
                        {formatMetric(
                          item.avgContextPrecision
                        )}
                      </td>

                      <td className="px-4 py-3">
                        {formatMetric(
                          item.avgContextRecall
                        )}
                      </td>

                      <td className="px-4 py-3">
                        {formatPercentage(
                          item.retrievalHitRate
                        )}
                      </td>

                      <td className="px-4 py-3">
                        {formatMetric(
                          item.avgLatencyMs,
                          1
                        )}
                      </td>
                    </tr>
                  )
                )}

                {results.length === 0 ? (
                  <tr>
                    <td
                      colSpan="12"
                      className="px-4 py-6 text-center text-text-secondary"
                    >
                      Chưa có kết quả
                      benchmark phù hợp.
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