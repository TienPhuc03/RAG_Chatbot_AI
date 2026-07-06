import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";

function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="max-w-xl rounded-[2rem] border border-border-subtle bg-white p-10 text-center shadow-soft">
        <div className="text-sm font-semibold uppercase tracking-[0.2em] text-text-muted">404</div>
        <h1 className="mt-5 text-5xl font-semibold tracking-[-0.03em] text-text-primary">Không tìm thấy trang</h1>
        <p className="mt-4 text-lg leading-8 text-text-secondary">
          Trang bạn yêu cầu hiện chưa tồn tại trong workspace này.
        </p>
        <Link
          to="/chat"
          className="mt-8 inline-flex items-center gap-3 rounded-2xl bg-teal px-5 py-3 text-base font-semibold text-white transition hover:bg-teal-strong"
        >
          <ArrowLeft className="size-4" />
          <span>Quay lại trợ lý</span>
        </Link>
      </div>
    </div>
  );
}

export default NotFoundPage;
