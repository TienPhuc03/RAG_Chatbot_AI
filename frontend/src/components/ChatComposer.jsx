import { ArrowUp, LoaderCircle, Mic, Paperclip } from "lucide-react";
import { useEffect, useRef } from "react";

function statusLabel(status) {
  if (status === "INDEXED") {
    return "San sang";
  }
  if (status === "FAILED") {
    return "That bai";
  }
  if (status === "PROCESSING") {
    return "Dang index";
  }
  if (status === "PENDING") {
    return "Dang tai len";
  }
  return status || "";
}

function ChatComposer({
  value,
  onChange,
  onSubmit,
  loading,
  selectedFile,
  attachments = [],
  attachmentNotice = "",
  onPickFile,
  onRemoveSelectedFile,
}) {
  const textareaRef = useRef(null);
  const fileInputRef = useRef(null);

  useEffect(() => {
    const textarea = textareaRef.current;

    if (!textarea) {
      return;
    }

    textarea.style.height = "0px";
    textarea.style.height = `${Math.min(textarea.scrollHeight, 220)}px`;
  }, [value]);

  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      onSubmit();
    }
  };

  return (
    <div className="sticky bottom-0 z-10 px-4 pb-5 pt-4 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-4xl rounded-[2rem] border border-border-subtle bg-white/95 p-3 shadow-float backdrop-blur-xl">
        <div className="flex items-end gap-3">
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="flex size-12 shrink-0 items-center justify-center rounded-2xl text-text-secondary transition hover:bg-surface-muted hover:text-text-primary"
          >
            <Paperclip className="size-5" />
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf,.doc,.docx,.ppt,.pptx"
            onChange={(event) => {
              const file = event.target.files?.[0] || null;
              onPickFile(file);
              event.target.value = "";
            }}
            className="hidden"
          />
          <textarea
            ref={textareaRef}
            rows={1}
            value={value}
            onChange={(event) => onChange(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Hoi toi bat cu dieu gi..."
            className="max-h-56 min-h-12 flex-1 resize-none bg-transparent px-2 py-3 text-lg text-text-primary outline-none placeholder:text-text-muted"
          />
          <button
            type="button"
            className="hidden size-12 shrink-0 items-center justify-center rounded-2xl text-text-secondary transition hover:bg-surface-muted hover:text-text-primary sm:flex"
          >
            <Mic className="size-5" />
          </button>
          <button
            type="button"
            disabled={loading || !value.trim()}
            onClick={onSubmit}
            className="flex size-12 shrink-0 items-center justify-center rounded-full bg-teal text-white transition hover:bg-teal-strong disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {loading ? <LoaderCircle className="size-5 animate-spin" /> : <ArrowUp className="size-5" />}
          </button>
        </div>

        {selectedFile ? (
          <div className="mt-3 flex flex-wrap gap-2">
            <div className="inline-flex items-center gap-2 rounded-full border border-border-subtle bg-surface-muted px-3 py-2 text-sm text-text-primary">
              <Paperclip className="size-4 text-teal" />
              <span className="max-w-60 truncate underline decoration-dotted underline-offset-4">
                {selectedFile.name}
              </span>
              <span className="text-xs text-text-secondary">Sap gui</span>
              <button
                type="button"
                onClick={onRemoveSelectedFile}
                className="rounded-full px-2 py-1 text-xs text-text-secondary transition hover:bg-white hover:text-text-primary"
              >
                Bo
              </button>
            </div>
          </div>
        ) : null}

        {attachments.length > 0 ? (
          <div className="mt-3 flex flex-wrap gap-2">
            {attachments.map((attachment) => (
              <div
                key={attachment.documentId}
                className="inline-flex items-center gap-2 rounded-full border border-border-subtle bg-white px-3 py-2 text-sm text-text-primary"
              >
                <Paperclip className="size-4 text-teal" />
                <span className="max-w-60 truncate underline decoration-dotted underline-offset-4">
                  {attachment.fileName}
                </span>
                <span className="text-xs text-text-secondary">{statusLabel(attachment.status)}</span>
              </div>
            ))}
          </div>
        ) : null}

        {attachmentNotice ? (
          <div className="mt-3 px-1 text-sm text-text-secondary">{attachmentNotice}</div>
        ) : null}
      </div>
    </div>
  );
}

export default ChatComposer;
