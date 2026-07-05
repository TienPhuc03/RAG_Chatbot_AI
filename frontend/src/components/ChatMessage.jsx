import { Bot, CircleAlert, Sparkles, User } from "lucide-react";

function buildCitationLabel(citation) {
  if (citation.sourceFileName && citation.pageNumber != null) {
    return `${citation.sourceFileName} • page ${citation.pageNumber}`;
  }

  if (citation.sourceFileName) {
    return citation.sourceFileName;
  }

  if (citation.pageNumber != null) {
    return `Page ${citation.pageNumber}`;
  }

  if (citation.courseCode && citation.chapterCode) {
    return `${citation.courseCode} • ${citation.chapterCode}`;
  }

  if (citation.courseCode) {
    return citation.courseCode;
  }

  if (citation.documentId) {
    return `Document ${citation.documentId.slice(0, 8)}`;
  }

  return "Structured source";
}

function ChatMessage({ message }) {
  const isUser = message.role === "USER";
  const isError = message.role === "ERROR";
  const citations = Array.isArray(message.citations) ? message.citations : [];

  return (
    <div className={`flex gap-4 ${isUser ? "justify-end" : "justify-start"}`}>
      {!isUser && (
        <div
          className={`mt-1 flex size-10 shrink-0 items-center justify-center rounded-2xl ${
            isError ? "bg-danger-soft text-rose-500" : "bg-teal text-white"
          }`}
        >
          {isError ? <CircleAlert className="size-5" /> : <Sparkles className="size-5" />}
        </div>
      )}

      <div className={`max-w-3xl space-y-3 ${isUser ? "items-end" : "items-start"}`}>
        <div
          className={`rounded-[1.75rem] border px-5 py-4 shadow-soft ${
            isUser
              ? "border-border-subtle bg-surface-muted text-text-primary"
              : isError
                ? "border-rose-200 bg-white text-rose-600"
                : "border-border-subtle bg-white text-text-primary"
          }`}
        >
          <p className="whitespace-pre-wrap text-base leading-8">{message.content}</p>
          {message.groundedInDocuments && !isUser && !isError ? (
            <div className="mt-4 inline-flex items-center gap-2 rounded-full bg-teal-soft px-3 py-1 text-xs font-semibold text-teal">
              <Bot className="size-3.5" />
              Grounded in uploaded documents
            </div>
          ) : null}
          {!isUser && !isError && citations.length > 0 ? (
            <div className="mt-4 space-y-2 border-t border-border-subtle pt-4">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-text-muted">
                Sources
              </p>
              <div className="flex flex-wrap gap-2">
                {citations.map((citation, index) => (
                  <div
                    key={`${citation.documentId || "doc"}-${citation.chunkId || index}`}
                    className="rounded-2xl bg-surface-muted px-3 py-2 text-xs text-text-secondary"
                  >
                    {buildCitationLabel(citation)}
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </div>
        <div className="flex items-center gap-2 px-1 text-xs text-text-muted">
          {isUser ? <User className="size-3.5" /> : <Sparkles className="size-3.5" />}
          <span>
            {message.createdAt ? new Date(message.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "Just now"}
          </span>
        </div>
      </div>
    </div>
  );
}

export default ChatMessage;
