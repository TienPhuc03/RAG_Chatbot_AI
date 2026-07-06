import { createContext, useCallback, useContext, useEffect, useState } from "react";
import {
  deleteDocument,
  fetchDocumentChunks,
  fetchDocuments,
  fetchDocumentStatus,
  uploadDocument,
} from "../services/documentService";

const DocumentsContext = createContext(null);

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function DocumentsProvider({ children }) {
  const [courseCode, setCourseCode] = useState("");
  const [courseName, setCourseName] = useState("");
  const [chapterCode, setChapterCode] = useState("");
  const [chapterTitle, setChapterTitle] = useState("");
  const [chunkingStrategy, setChunkingStrategy] = useState("SEMANTIC");
  const [embeddingModel, setEmbeddingModel] = useState("GEMINI_EMBEDDING_001");
  const [file, setFile] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [selectedChunks, setSelectedChunks] = useState([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState("");
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");

  const loadDocuments = useCallback(async (filterCourseCode = "") => {
    setLoading(true);
    try {
      const data = await fetchDocuments(filterCourseCode);
      setDocuments(data);
    } catch (error) {
      setMessage(error.message || "Không tải được danh sách tài liệu.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDocuments();
  }, [loadDocuments]);

  const startUpload = useCallback(async () => {
    if (uploading) {
      return;
    }

    if (!file) {
      setMessage("Bạn chưa chọn tệp.");
      return;
    }
    if (!courseCode.trim()) {
      setMessage("Bạn cần nhập mã học phần.");
      return;
    }
    if (!courseName.trim()) {
      setMessage("Bạn cần nhập tên học phần.");
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
        embeddingModel,
      });

      setFile(null);
      setMessage(`Đã nhận tệp: ${response.title}. Đang chờ index...`);

      let currentStatus = response.status;
      let finalFailureReason = "";
      while (currentStatus === "PENDING" || currentStatus === "PROCESSING") {
        await wait(2000);
        const status = await fetchDocumentStatus(response.id);
        currentStatus = status.status;
        finalFailureReason = status.failureReason || "";
      }

      await loadDocuments(courseCode.trim());
      if (currentStatus === "FAILED" && finalFailureReason) {
        setMessage(`Index thất bại: ${finalFailureReason}`);
      } else {
        setMessage(`Index xong với trạng thái: ${currentStatus}`);
      }
    } catch (error) {
      setMessage(error.message || "Upload thất bại.");
    } finally {
      setUploading(false);
    }
  }, [
    chapterCode,
    chapterTitle,
    chunkingStrategy,
    courseCode,
    courseName,
    embeddingModel,
    file,
    loadDocuments,
    uploading,
  ]);

  const previewChunks = useCallback(async (documentId) => {
    try {
      const chunks = await fetchDocumentChunks(documentId);
      setSelectedDocumentId(documentId);
      setSelectedChunks(chunks);
    } catch (error) {
      setMessage(error.message || "Không tải được bản xem trước chunk.");
    }
  }, []);

  const removeDocument = useCallback(
    async (documentId) => {
      const confirmed = window.confirm("Xóa tài liệu này khỏi danh sách và vector index?");
      if (!confirmed) {
        return;
      }

      try {
        await deleteDocument(documentId);
        if (selectedDocumentId === documentId) {
          setSelectedDocumentId("");
          setSelectedChunks([]);
        }
        setMessage("Đã xóa tài liệu.");
        await loadDocuments(courseCode.trim());
      } catch (error) {
        setMessage(error.message || "Không xóa được tài liệu.");
      }
    },
    [courseCode, loadDocuments, selectedDocumentId]
  );

  return (
    <DocumentsContext.Provider
      value={{
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
        file,
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
      }}
    >
      {children}
    </DocumentsContext.Provider>
  );
}

export function useDocuments() {
  const context = useContext(DocumentsContext);

  if (!context) {
    throw new Error("useDocuments must be used within DocumentsProvider");
  }

  return context;
}
