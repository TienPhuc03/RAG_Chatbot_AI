import request from "./apiClient";

export function fetchConversations() {
  return request("/api/chat/conversations");
}

export function fetchChatHistory(sessionId) {
  return request(`/api/chat/history/${sessionId}`);
}

export function sendMessage(payload) {
  return request("/api/chat/message", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function fetchChatAttachments(sessionId) {
  return request(`/api/chat/attachments/${sessionId}`);
}

export async function uploadChatAttachment(payload) {
  const formData = new FormData();
  if (payload.sessionId) {
    formData.append("sessionId", payload.sessionId);
  }
  formData.append("file", payload.file);

  const response = await fetch("/api/chat/attachments", {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    let message = "Upload file that bai.";
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
