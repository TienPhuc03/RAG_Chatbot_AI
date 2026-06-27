const AUTH_KEY = "rag-chatbot-auth";

export function isAuthenticated() {
  return sessionStorage.getItem(AUTH_KEY) === "true";
}

export function createAuthSession() {
  sessionStorage.setItem(AUTH_KEY, "true");
}

export function clearAuthSession() {
  sessionStorage.removeItem(AUTH_KEY);
}
