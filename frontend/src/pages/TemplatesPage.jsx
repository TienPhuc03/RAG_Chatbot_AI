import { ArrowRight, Sparkles, WandSparkles } from "lucide-react";
import SectionIntro from "../components/SectionIntro";

const templates = [
  {
    title: "Lecture Summary",
    description: "Turn a long chapter into a concise revision sheet with key takeaways and terminology.",
  },
  {
    title: "Code Review Buddy",
    description: "Ask the assistant to inspect a snippet, surface risks, and explain cleanups clearly.",
  },
  {
    title: "Exam Drill",
    description: "Generate practice questions from uploaded content and get instant rationale for each answer.",
  },
];

function TemplatesPage() {
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <SectionIntro
        eyebrow="Prompt Library"
        title="Templates"
        description="A starter collection for common classroom and engineering workflows. Each template keeps the tone aligned with the clean assistant experience."
        action={
          <button className="inline-flex h-13 items-center gap-3 rounded-2xl bg-teal px-5 text-base font-semibold text-white transition hover:bg-teal-strong">
            <Sparkles className="size-5" />
            <span>Create template</span>
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
              <span>Use template</span>
              <ArrowRight className="size-4" />
            </button>
          </article>
        ))}
      </div>
    </div>
  );
}

export default TemplatesPage;
