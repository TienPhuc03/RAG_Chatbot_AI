import { Menu, MoreVertical, Share2 } from "lucide-react";
import { useLocation } from "react-router-dom";

const PAGE_TITLES = {
  "/chat": "Trợ lý",
  "/documents": "Tài liệu",
  "/benchmark": "Đánh giá",
};

function AppHeader({ onOpenSidebar }) {
  const location = useLocation();
  const title =
    location.pathname.startsWith("/chat/")
      ? "Trợ lý"
      : PAGE_TITLES[location.pathname] || "Không gian làm việc";

  return (
    <header className="z-20 shrink-0 border-b border-border-subtle bg-white/80 backdrop-blur-xl">
      <div className="flex items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onOpenSidebar}
            className="flex size-11 items-center justify-center rounded-2xl border border-border-subtle bg-white text-text-primary transition hover:bg-surface-muted md:hidden"
          >
            <Menu className="size-5" />
          </button>
          <div>
            <h1 className="text-3xl font-semibold tracking-[-0.03em] text-text-primary">{title}</h1>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            className="hidden size-11 items-center justify-center rounded-2xl border border-transparent text-text-secondary transition hover:border-border-subtle hover:bg-surface-muted sm:flex"
          >
            <Share2 className="size-5" />
          </button>
          <button
            type="button"
            className="hidden size-11 items-center justify-center rounded-2xl border border-transparent text-text-secondary transition hover:border-border-subtle hover:bg-surface-muted sm:flex"
          >
            <MoreVertical className="size-5" />
          </button>
          <div className="flex size-12 items-center justify-center rounded-full bg-[linear-gradient(135deg,_#fed7aa,_#f5d0fe,_#bfdbfe)] text-sm font-bold text-slate-700 shadow-soft">
            AN
          </div>
        </div>
      </div>
    </header>
  );
}

export default AppHeader;
