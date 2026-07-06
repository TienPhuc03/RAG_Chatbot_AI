import SectionIntro from "../components/SectionIntro";
import { useDocuments } from "../context/DocumentsContext";

const STRATEGIES = ["FIXED_SIZE", "HIERARCHICAL", "SEMANTIC"];
const EMBEDDING_MODELS = [
  "GEMINI_EMBEDDING_001",
  "MULTILINGUAL_E5_BASE",
  "PHOBERT_BASE",
  "BGE_M3",
];

function DocumentsPage() {
  const {
    courseCode,
    setCourseCode,
    courseName,
    setCourseName,
    chapterCode,
    setChapterCode,
    chapterTitle,
    setChapterTitle,
    chunkingStrategy,
    setChunkingStrategy,
    embeddingModel,
    setEmbeddingModel,
    setFile,
    documents,
    selectedChunks,
    selectedDocumentId,
    loading,
    uploading,
    message,
    loadDocuments,
    startUpload,
    previewChunks,
    removeDocument,
  } = useDocuments();

  async function handleUpload(event) {
    event.preventDefault();
    await startUpload();
  }

  return (
    <div className="scrollbar-subtle h-full overflow-y-auto px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto w-full max-w-7xl">
      <SectionIntro
        eyebrow="Quy trình nghiên cứu"
        title="Tài liệu"
        description="Upload tài liệu, chọn chunking và embedding model, rồi giữ cùng một corpus cho mỗi cấu hình benchmark."
      />

      <form
        onSubmit={handleUpload}
        className="mt-8 grid gap-4 rounded-[2rem] border border-border-subtle bg-white p-6 shadow-soft lg:grid-cols-3"
      >
        <label className="text-sm text-text-secondary">
          Tệp
          <input
            type="file"
            accept=".pdf,.doc,.docx,.ppt,.pptx"
            onChange={(event) => setFile(event.target.files?.[0] || null)}
            className="mt-2 block h-11 w-full rounded-2xl border border-border-subtle px-4 py-2"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Mã học phần
          <input
            value={courseCode}
            onChange={(event) => setCourseCode(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Tên học phần
          <input
            value={courseName}
            onChange={(event) => setCourseName(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Mã chương
          <input
            value={chapterCode}
            onChange={(event) => setChapterCode(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Tên chương
          <input
            value={chapterTitle}
            onChange={(event) => setChapterTitle(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Chiến lược chunking
          <select
            value={chunkingStrategy}
            onChange={(event) => setChunkingStrategy(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          >
            {STRATEGIES.map((strategy) => (
              <option key={strategy} value={strategy}>
                {strategy}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm text-text-secondary">
          Mô hình embedding
          <select
            value={embeddingModel}
            onChange={(event) => setEmbeddingModel(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          >
            {EMBEDDING_MODELS.map((model) => (
              <option key={model} value={model}>
                {model}
              </option>
            ))}
          </select>
        </label>
        <div className="flex flex-wrap gap-3 lg:col-span-3">
          <button
            type="submit"
            disabled={uploading}
            className="h-12 rounded-2xl bg-teal px-5 text-sm font-semibold text-white transition hover:bg-teal-strong disabled:bg-slate-300"
          >
            {uploading ? "Đang upload..." : "Upload và index"}
          </button>
          <button
            type="button"
            onClick={() => loadDocuments(courseCode.trim())}
            className="h-12 rounded-2xl border border-border-subtle bg-white px-5 text-sm font-semibold text-text-primary"
          >
            Lọc danh sách
          </button>
        </div>
      </form>

      {message ? (
        <div className="mt-4 rounded-2xl border border-border-subtle bg-white px-5 py-4 text-sm text-text-secondary shadow-soft">
          {message}
        </div>
      ) : null}

      <div className="mt-8 overflow-hidden rounded-[2rem] border border-border-subtle bg-white shadow-soft">
        <div className="border-b border-border-subtle px-6 py-4 text-lg font-semibold text-text-primary">
          Danh sách tài liệu
        </div>
        {loading ? (
          <div className="px-6 py-5 text-text-secondary">Đang tải dữ liệu...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-surface-soft text-text-secondary">
                <tr>
                  <th className="px-4 py-3">Tiêu đề</th>
                  <th className="px-4 py-3">Học phần</th>
                  <th className="px-4 py-3">Chương</th>
                  <th className="px-4 py-3">Trạng thái</th>
                  <th className="px-4 py-3">Lý do</th>
                  <th className="px-4 py-3">Chiến lược</th>
                  <th className="px-4 py-3">Embedding</th>
                  <th className="px-4 py-3">Chunks</th>
                  <th className="px-4 py-3">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {documents.map((document) => (
                  <tr key={document.id} className="border-t border-border-subtle">
                    <td className="px-4 py-3">{document.title}</td>
                    <td className="px-4 py-3">{document.courseCode}</td>
                    <td className="px-4 py-3">{document.chapterCode || "-"}</td>
                    <td className="px-4 py-3">{document.status}</td>
                    <td className="px-4 py-3 text-xs text-text-secondary">
                      {document.failureReason || "-"}
                    </td>
                    <td className="px-4 py-3">{document.latestChunkingStrategy || "-"}</td>
                    <td className="px-4 py-3">{document.embeddingModel || "-"}</td>
                    <td className="px-4 py-3">{document.chunkCount ?? 0}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => previewChunks(document.id)}
                        className="rounded-xl bg-teal-soft px-3 py-2 text-xs font-semibold text-teal"
                      >
                        Xem chunk
                      </button>
                        <button
                          type="button"
                          onClick={() => removeDocument(document.id)}
                          className="rounded-xl border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-600"
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {documents.length === 0 ? (
                  <tr>
                    <td colSpan="9" className="px-4 py-4 text-text-secondary">
                      Chưa có tài liệu nào.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedChunks.length > 0 ? (
        <div className="mt-8 rounded-[2rem] border border-border-subtle bg-white p-6 shadow-soft">
          <div className="text-lg font-semibold text-text-primary">
            Chunk preview: {selectedDocumentId}
          </div>
          <div className="mt-4 space-y-4">
            {selectedChunks.map((chunk) => (
              <div
                key={chunk.chunkIndex}
                className="rounded-2xl border border-border-subtle bg-surface-muted p-4"
              >
                <div className="text-sm font-semibold text-text-primary">
                  Chunk #{chunk.chunkIndex} | {chunk.chunkingStrategy} | token: {chunk.tokenCount ?? "-"}
                </div>
                <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-text-secondary">
                  {chunk.content}
                </p>
              </div>
            ))}
          </div>
        </div>
      ) : null}
      </div>
    </div>
  );
}

export default DocumentsPage;
