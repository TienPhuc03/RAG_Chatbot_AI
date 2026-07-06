import {
  ChevronRight,
  CircleHelp,
  Database,
  History,
  LogOut,
  MessageSquarePlus,
  Microscope,
} from "lucide-react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useConversations } from "../context/ConversationsContext";
import { clearAuthSession } from "../lib/auth";
import LogoMark from "./LogoMark";

function Sidebar({ mobileOpen, onClose }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { conversations, loading } = useConversations();

  const closeOnMobile = () => {
    if (onClose) {
      onClose();
    }
  };

  const handleSignOut = () => {
    clearAuthSession();
    closeOnMobile();
    navigate("/login", { replace: true });
  };

  const isChatRoute = location.pathname.startsWith("/chat");

  const items = [
    { to: "/chat", label: "Cuộc trò chuyện mới", icon: MessageSquarePlus },
    { to: "/documents", label: "Tài liệu", icon: Database },
    { to: "/benchmark", label: "Đánh giá", icon: Microscope },
  ];

  return (
    <>
      <div
        className={`fixed inset-0 z-30 bg-slate-950/20 backdrop-blur-sm transition md:hidden ${
          mobileOpen ? "pointer-events-auto opacity-100" : "pointer-events-none opacity-0"
        }`}
        onClick={onClose}
      />
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex h-screen w-[19rem] flex-col border-r border-border-subtle bg-white/90 px-5 py-6 shadow-soft backdrop-blur-xl transition md:w-[21rem] md:translate-x-0 md:shadow-none ${
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center gap-4">
          <LogoMark variant="auth" className="size-12 rounded-xl" />
          <div>
            <div className="text-[2rem] font-bold tracking-[-0.03em] text-text-primary">Trợ lý AI</div>
          </div>
        </div>

        <nav className="mt-12 space-y-2">
          {items.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/chat"}
              onClick={closeOnMobile}
              className={({ isActive }) =>
                `flex items-center gap-4 rounded-2xl px-3 py-3 text-lg transition ${
                  isActive || (to === "/chat" && isChatRoute)
                    ? "bg-surface-soft text-text-primary"
                    : "text-text-secondary hover:bg-surface-muted hover:text-text-primary"
                }`
              }
            >
              <Icon className="size-5" strokeWidth={2.1} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="mt-8 flex min-h-0 flex-1 flex-col">
          <div className="mb-3 flex items-center gap-3 px-3 text-sm font-semibold uppercase tracking-[0.18em] text-text-muted">
            <History className="size-4" />
            <span>Lịch sử</span>
          </div>
          <div className="scrollbar-subtle min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
            {loading ? (
              <div className="rounded-2xl border border-border-subtle bg-surface-muted px-4 py-4 text-sm text-text-secondary">
                Đang tải cuộc trò chuyện...
              </div>
            ) : conversations.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-border-subtle bg-surface px-4 py-4 text-sm leading-6 text-text-secondary">
                Các cuộc trò chuyện gần đây sẽ xuất hiện ở đây sau tin nhắn đầu tiên.
              </div>
            ) : (
              conversations.map((conversation) => (
                <NavLink
                  key={conversation.sessionId}
                  to={`/chat/${conversation.sessionId}`}
                  onClick={closeOnMobile}
                  className={({ isActive }) =>
                    `block rounded-2xl border px-4 py-3 transition ${
                      isActive
                        ? "border-teal-strong/30 bg-teal-soft/45"
                        : "border-transparent bg-white hover:border-border-subtle hover:bg-surface-muted"
                    }`
                  }
                >
                  <div className="line-clamp-2 text-sm font-semibold text-text-primary">
                    {conversation.title || "Cuộc trò chuyện chưa đặt tên"}
                  </div>
                  <div className="mt-2 text-xs text-text-muted">
                    {new Date(conversation.updatedAt).toLocaleString("vi-VN")}
                  </div>
                </NavLink>
              ))
            )}
          </div>
        </div>

        <div className="mt-6 space-y-2 border-t border-border-subtle pt-5">
          <button
            type="button"
            className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left text-lg text-text-secondary transition hover:bg-surface-muted hover:text-text-primary"
          >
            <CircleHelp className="size-5" />
            <span>Trợ giúp</span>
          </button>
          <button
            type="button"
            onClick={handleSignOut}
            className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left text-lg text-text-secondary transition hover:bg-surface-muted hover:text-text-primary"
          >
            <LogOut className="size-5" />
            <span>Đăng xuất</span>
          </button>
          <button
            type="button"
            className="flex w-full items-center justify-between rounded-2xl px-3 py-3 text-left text-sm font-medium text-text-secondary transition hover:bg-surface-muted hover:text-text-primary md:hidden"
            onClick={closeOnMobile}
          >
            <span>Đóng menu</span>
            <ChevronRight className="size-4" />
          </button>
        </div>
      </aside>
    </>
  );
}

export default Sidebar;
