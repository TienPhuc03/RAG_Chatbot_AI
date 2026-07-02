import { useEffect, useState } from "react";
import SectionIntro from "../components/SectionIntro";
import {
  fetchDocumentChunks,
  fetchDocuments,
  fetchDocumentStatus,
  uploadDocument,
} from "../services/documentService";

const STRATEGIES = ["FIXED_SIZE", "HIERARCHICAL", "SEMANTIC"];

function DocumentsPage() {
  const [courseCode, setCourseCode] = useState("");
  const [courseName, setCourseName] = useState("");
  const [chapterCode, setChapterCode] = useState("");
  const [chapterTitle, setChapterTitle] = useState("");
  const [chunkingStrategy, setChunkingStrategy] = useState("SEMANTIC");
  const [file, setFile] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [selectedChunks, setSelectedChunks] = useState([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState("");
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");

  async function loadDocuments(filterCourseCode = "") {
    setLoading(true);
    try {
      const data = await fetchDocuments(filterCourseCode);
      setDocuments(data);
    } catch (error) {
      setMessage(error.message || "Khong tai duoc danh sach tai lieu.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDocuments();
  }, []);

  async function handleUpload(event) {
    event.preventDefault();

    if (!file) {
      setMessage("Ban chua chon file.");
      return;
    }
    if (!courseCode.trim()) {
      setMessage("Ban can nhap course code.");
      return;
    }
    if (!courseName.trim()) {
      setMessage("Ban can nhap course name.");
      return;
    }

    setUploading(true);
    setMessage("");

    try {
      const response = await uploadDocument({
        file,
        courseCode,
        courseName,
        chapterCode,
        chapterTitle,
        chunkingStrategy,
      });

      setMessage(`Da nhan file: ${response.title}. Dang cho index...`);

      let currentStatus = response.status;
      let finalFailureReason = "";
      while (currentStatus === "PENDING" || currentStatus === "PROCESSING") {
        await new Promise((resolve) => setTimeout(resolve, 2000));
        const status = await fetchDocumentStatus(response.id);
        currentStatus = status.status;
        finalFailureReason = status.failureReason || "";
      }

      await loadDocuments(courseCode.trim());
      if (currentStatus === "FAILED" && finalFailureReason) {
        setMessage(`Index that bai: ${finalFailureReason}`);
      } else {
        setMessage(`Index xong voi trang thai: ${currentStatus}`);
      }
    } catch (error) {
      setMessage(error.message || "Upload that bai.");
    } finally {
      setUploading(false);
    }
  }

  async function handlePreviewChunks(documentId) {
    try {
      const chunks = await fetchDocumentChunks(documentId);
      setSelectedDocumentId(documentId);
      setSelectedChunks(chunks);
    } catch (error) {
      setMessage(error.message || "Khong tai duoc chunk preview.");
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <SectionIntro
        eyebrow="Research Flow"
        title="Documents"
        description="Upload tai lieu, chon chien luoc chunking va kiem tra chunk preview ngay tren giao dien."
      />

      <form
        onSubmit={handleUpload}
        className="mt-8 grid gap-4 rounded-[2rem] border border-border-subtle bg-white p-6 shadow-soft lg:grid-cols-3"
      >
        <label className="text-sm text-text-secondary">
          File
          <input
            type="file"
            accept=".pdf,.doc,.docx,.ppt,.pptx"
            onChange={(event) => setFile(event.target.files?.[0] || null)}
            className="mt-2 block h-11 w-full rounded-2xl border border-border-subtle px-4 py-2"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Course code
          <input
            value={courseCode}
            onChange={(event) => setCourseCode(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Course name
          <input
            value={courseName}
            onChange={(event) => setCourseName(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Chapter code
          <input
            value={chapterCode}
            onChange={(event) => setChapterCode(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Chapter title
          <input
            value={chapterTitle}
            onChange={(event) => setChapterTitle(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4"
          />
        </label>
        <label className="text-sm text-text-secondary">
          Chunking strategy
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
        <div className="flex flex-wrap gap-3 lg:col-span-3">
          <button
            type="submit"
            disabled={uploading}
            className="h-12 rounded-2xl bg-teal px-5 text-sm font-semibold text-white transition hover:bg-teal-strong disabled:bg-slate-300"
          >
            {uploading ? "Dang upload..." : "Upload va index"}
          </button>
          <button
            type="button"
            onClick={() => loadDocuments(courseCode.trim())}
            className="h-12 rounded-2xl border border-border-subtle bg-white px-5 text-sm font-semibold text-text-primary"
          >
            Loc danh sach
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
          Danh sach tai lieu
        </div>
        {loading ? (
          <div className="px-6 py-5 text-text-secondary">Dang tai du lieu...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-surface-soft text-text-secondary">
                <tr>
                  <th className="px-4 py-3">Title</th>
                  <th className="px-4 py-3">Course</th>
                  <th className="px-4 py-3">Chapter</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Reason</th>
                  <th className="px-4 py-3">Strategy</th>
                  <th className="px-4 py-3">Chunks</th>
                  <th className="px-4 py-3">Action</th>
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
                    <td className="px-4 py-3">{document.chunkCount ?? 0}</td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => handlePreviewChunks(document.id)}
                        className="rounded-xl bg-teal-soft px-3 py-2 text-xs font-semibold text-teal"
                      >
                        Xem chunk
                      </button>
                    </td>
                  </tr>
                ))}
                {documents.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="px-4 py-4 text-text-secondary">
                      Chua co tai lieu nao.
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
  );
}

export default DocumentsPage;
