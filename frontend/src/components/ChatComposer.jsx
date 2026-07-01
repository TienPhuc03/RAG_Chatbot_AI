import { ArrowUp, LoaderCircle, Mic, Paperclip } from "lucide-react";
import { useEffect, useRef } from "react";

function ChatComposer({ value, onChange, onSubmit, loading }) {
  const textareaRef = useRef(null);

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
            className="flex size-12 shrink-0 items-center justify-center rounded-2xl text-text-secondary transition hover:bg-surface-muted hover:text-text-primary"
          >
            <Paperclip className="size-5" />
          </button>
          <textarea
            ref={textareaRef}
            rows={1}
            value={value}
            onChange={(event) => onChange(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Hỏi tôi bất cứ điều gì..."
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
      </div>
    </div>
  );
}

export default ChatComposer;
