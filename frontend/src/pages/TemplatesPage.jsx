import { ArrowRight, Sparkles, WandSparkles } from "lucide-react";
import SectionIntro from "../components/SectionIntro";

const templates = [
  {
    title: "Tóm tắt bài học",
    description: "Biến một chương dài thành bản ôn tập ngắn gọn với ý chính và thuật ngữ quan trọng.",
  },
  {
    title: "Bạn đồng hành review code",
    description: "Nhờ trợ lý kiểm tra đoạn code, chỉ ra rủi ro và giải thích cách chỉnh sửa rõ ràng.",
  },
  {
    title: "Luyện đề",
    description: "Tạo câu hỏi luyện tập từ nội dung đã upload và nhận giải thích nhanh cho từng đáp án.",
  },
];

function TemplatesPage() {
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <SectionIntro
        eyebrow="Thư viện prompt"
        title="Mẫu prompt"
        description="Bộ mẫu khởi đầu cho các quy trình học tập và kỹ thuật thường gặp."
        action={
          <button className="inline-flex h-13 items-center gap-3 rounded-2xl bg-teal px-5 text-base font-semibold text-white transition hover:bg-teal-strong">
            <Sparkles className="size-5" />
            <span>Tạo mẫu</span>
          </button>
        }
      />

      <div className="mt-8 grid gap-6 lg:grid-cols-3">
        {templates.map((template) => (
          <article key={template.title} className="rounded-[2rem] border border-border-subtle bg-white p-6 shadow-soft">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-teal-soft text-teal">
              <WandSparkles className="size-5" />
            </div>
            <h3 className="mt-6 text-2xl font-semibold tracking-[-0.03em] text-text-primary">{template.title}</h3>
            <p className="mt-4 text-base leading-7 text-text-secondary">{template.description}</p>
            <button className="mt-8 inline-flex items-center gap-2 text-sm font-semibold text-teal transition hover:text-teal-strong">
              <span>Dùng mẫu</span>
              <ArrowRight className="size-4" />
            </button>
          </article>
        ))}
      </div>
    </div>
  );
}

export default TemplatesPage;
