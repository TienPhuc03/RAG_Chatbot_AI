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
