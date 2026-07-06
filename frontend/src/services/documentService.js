import request from "./apiClient";

export function fetchDocuments(courseCode = "") {
  const query = courseCode ? `?courseCode=${encodeURIComponent(courseCode)}` : "";
  return request(`/api/documents${query}`);
}

export function fetchDocumentStatus(documentId) {
  return request(`/api/documents/${documentId}/status`);
}

export function fetchDocumentChunks(documentId) {
  return request(`/api/documents/${documentId}/chunks`);
}

export function deleteDocument(documentId) {
  return request(`/api/documents/${documentId}`, {
    method: "DELETE",
  });
}

export async function uploadDocument(payload) {
  const formData = new FormData();
  formData.append("file", payload.file);
  formData.append("courseCode", payload.courseCode);
  formData.append("courseName", payload.courseName);
  formData.append("chapterCode", payload.chapterCode || "");
  formData.append("chapterTitle", payload.chapterTitle || "");
  formData.append("chunkingStrategy", payload.chunkingStrategy);
  if (payload.embeddingModel) {
    formData.append("embeddingModel", payload.embeddingModel);
  }

  const response = await fetch("/api/documents/upload", {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    let message = "Upload thất bại.";
    try {
      const body = await response.json();
      message = body.message || body.error || message;
    } catch (error) {
      const text = await response.text();
      if (text) {
        message = text;
      }
    }
    throw new Error(message);
  }

  return response.json();
}
