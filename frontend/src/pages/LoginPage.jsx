import { ArrowRight } from "lucide-react";
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import AuthInput from "../components/AuthInput";
import AuthShell from "../components/AuthShell";
import LogoMark from "../components/LogoMark";
import SocialButton from "../components/SocialButton";
import { createAuthSession } from "../lib/auth";

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const redirectTo = location.state?.from?.pathname || "/chat";

  const handleSubmit = (event) => {
    event.preventDefault();
    createAuthSession();
    navigate(redirectTo, { replace: true });
  };

  return (
    <AuthShell accent="left">
      <div className="mx-auto flex max-w-[34rem] flex-col items-center text-center">
        <LogoMark variant="auth" className="size-15 rounded-2xl" />
        <h1 className="mt-10 text-6xl font-semibold tracking-[-0.04em] text-text-primary">Chào mừng trở lại</h1>
        <p className="mt-4 text-[1.8rem] text-text-secondary">Tiếp tục làm việc cùng Trợ lý AI</p>

        <form
          onSubmit={handleSubmit}
          className="mt-8 w-full rounded-[2rem] border border-border-subtle bg-white px-6 py-8 text-left shadow-soft sm:px-10"
        >
          <div className="space-y-7">
            <AuthInput
              label="Địa chỉ email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="name@company.com"
              required
            />
            <AuthInput
              label="Mật khẩu"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="••••••••"
              required
              action={
                <button type="button" className="text-base font-semibold text-teal transition hover:text-teal-strong">
                  Quên mật khẩu?
                </button>
              }
            />
          </div>

          <button
            type="submit"
            className="mt-8 flex h-15 w-full items-center justify-center gap-3 rounded-2xl bg-teal px-6 text-2xl font-semibold text-white transition hover:bg-teal-strong"
          >
            <span>Đăng nhập</span>
            <ArrowRight className="size-6" />
          </button>

          <div className="my-8 flex items-center gap-4 text-base text-text-secondary">
            <div className="h-px flex-1 bg-border-subtle" />
            <span>Hoặc tiếp tục với</span>
            <div className="h-px flex-1 bg-border-subtle" />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <SocialButton icon="G">Google</SocialButton>
            <SocialButton icon="◔">GitHub</SocialButton>
          </div>
        </form>

        <p className="mt-10 text-[1.8rem] text-text-secondary">
          Chưa có tài khoản?{" "}
          <Link to="/register" className="font-semibold text-teal transition hover:text-teal-strong">
            Tạo tài khoản
          </Link>
        </p>

        <div className="mt-16 flex flex-wrap items-center justify-center gap-x-10 gap-y-4 text-lg text-text-muted">
          <span>Chính sách bảo mật</span>
          <span>Điều khoản dịch vụ</span>
          <span>Trung tâm trợ giúp</span>
        </div>
      </div>
    </AuthShell>
  );
}

export default LoginPage;
