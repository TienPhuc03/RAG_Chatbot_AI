import { MessageSquareText, Sparkles } from "lucide-react";

function ChatWelcome({ onPromptSelect }) {
  const prompts = [
    "Tóm tắt tài liệu Java chương 1 cho mình.",
    "Giải thích sự khác nhau giữa OOP và Functional Programming.",
    "Gợi ý một lộ trình học Spring Boot trong 4 tuần.",
  ];

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-1 flex-col justify-center gap-8 px-4 py-10 sm:px-6 lg:px-8">
      <div className="space-y-4 text-center">
        <div className="mx-auto flex size-14 items-center justify-center rounded-2xl bg-teal text-white shadow-soft">
          <Sparkles className="size-6" />
        </div>
        <h2 className="text-4xl font-semibold tracking-[-0.03em] text-text-primary">
          Hôm nay bạn muốn tìm hiểu gì?
        </h2>
        <p className="mx-auto max-w-2xl text-base leading-7 text-text-secondary">
          Hỏi trực tiếp, đính kèm tài liệu, hoặc chọn nhanh một gợi ý bên dưới để bắt đầu.
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        {prompts.map((prompt) => (
          <button
            key={prompt}
            type="button"
            onClick={() => onPromptSelect(prompt)}
            className="rounded-2xl border border-border-subtle bg-white px-4 py-4 text-left text-sm font-medium leading-6 text-text-secondary shadow-soft transition hover:border-teal-strong/35 hover:bg-teal-soft/40 hover:text-text-primary"
          >
            <MessageSquareText className="mb-3 size-5 text-teal" />
            {prompt}
          </button>
        ))}
      </div>
    </div>
  );
}

export default ChatWelcome;
