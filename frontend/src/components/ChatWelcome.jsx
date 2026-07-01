import { Image, LayoutGrid, Sparkles } from "lucide-react";

function ChatWelcome({ onPromptSelect }) {
  const prompts = [
    "Tóm tắt tài liệu Java chương 1 cho mình.",
    "Giải thích sự khác nhau giữa OOP và Functional Programming.",
    "Gợi ý một lộ trình học Spring Boot trong 4 tuần.",
  ];

//   return (
//     <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-4 pb-10 pt-6 sm:px-6">
//       <div className="grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)]">
//         <div className="rounded-[2rem] border border-border-subtle bg-white p-8 shadow-soft">
//           <div className="text-sm font-semibold uppercase tracking-[0.2em] text-text-muted">Bio / 4 cols</div>
//           <div className="mt-10 rounded-[1.75rem] border border-dashed border-border-subtle bg-surface-muted p-6 text-base leading-7 text-text-secondary">
//             Clean, airy, and purpose-built for long-form conversation with your course materials.
//           </div>
//         </div>
//         <div className="rounded-[2rem] border border-border-subtle bg-white p-4 shadow-soft">
//           <div className="overflow-hidden rounded-[1.5rem] bg-[linear-gradient(135deg,_#111827_0%,_#273449_35%,_#e5ddd2_56%,_#738499_73%,_#111827_100%)] p-8">
//             <div className="flex h-[17rem] items-end justify-between rounded-[1.25rem] border border-white/10 bg-[linear-gradient(90deg,_rgba(255,255,255,0.05),_rgba(255,255,255,0.02))] px-6 pb-6">
//               <div className="space-y-3 text-white">
//                 <div className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em]">
//                   <Image className="size-3.5" />
//                   Visual Context
//                 </div>
//                 <h2 className="max-w-md text-3xl font-semibold tracking-[-0.03em]">
//                   Build a professional but modern AI workspace.
//                 </h2>
//               </div>
//               <div className="hidden rounded-2xl border border-white/15 bg-white/10 p-4 backdrop-blur xl:block">
//                 <LayoutGrid className="size-7 text-white" />
//               </div>
//             </div>
//           </div>
//         </div>
//       </div>

//       <div className="mx-auto max-w-4xl space-y-7">
//         <p className="text-[clamp(1.5rem,2vw,2.3rem)] leading-[1.45] tracking-[-0.03em] text-text-primary">
//           To implement this, keep your layout breathable with generous spacing and a restrained teal accent so the
//           content stays in focus.
//         </p>

//         <div className="ml-auto max-w-3xl rounded-[1.75rem] border border-border-subtle bg-white px-6 py-5 text-lg leading-8 text-text-primary shadow-soft">
//           Yes, please. Also, suggest a color palette that feels professional but modern.
//         </div>

//         <div className="space-y-4">
//           <div className="inline-flex items-center gap-3 rounded-full bg-white px-4 py-3 shadow-soft">
//             <div className="flex size-9 items-center justify-center rounded-xl bg-teal text-white">
//               <Sparkles className="size-4.5" />
//             </div>
//             <span className="text-sm font-semibold uppercase tracking-[0.18em] text-text-primary">Assistant</span>
//           </div>

//           <div className="overflow-hidden rounded-[1.75rem] border border-black bg-[#1a1f1c] shadow-float">
//             <div className="flex items-center justify-between border-b border-white/10 px-5 py-4 text-sm text-white/80">
//               <span className="font-medium">tailwind theme</span>
//               <span>Copy</span>
//             </div>
//             <pre className="overflow-x-auto px-5 py-6 text-sm leading-7 text-[#8cf7ea]">
//               <code>{`colors: {
//   background: '#F9F9FF',
//   textPrimary: '#111827',
//   textSecondary: '#4B5563',
//   accentTeal: '#0D9488'
// }`}</code>
//             </pre>
//           </div>
//         </div>

//         <div className="flex flex-wrap gap-3 pt-3">
//           {prompts.map((prompt) => (
//             <button
//               key={prompt}
//               type="button"
//               onClick={() => onPromptSelect(prompt)}
//               className="rounded-full border border-border-subtle bg-white px-5 py-3 text-left text-sm font-medium text-text-secondary transition hover:border-teal-strong/35 hover:bg-teal-soft/40 hover:text-text-primary"
//             >
//               {prompt}
//             </button>
//           ))}
//         </div>
//       </div>
//     </div>
//   );
// }
}
export default ChatWelcome;
