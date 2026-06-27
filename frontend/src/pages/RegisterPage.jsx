import { ArrowRight } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthInput from "../components/AuthInput";
import AuthShell from "../components/AuthShell";
import LogoMark from "../components/LogoMark";
import SocialButton from "../components/SocialButton";
import { createAuthSession } from "../lib/auth";

function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const handleChange = (field) => (event) => {
    setForm((current) => ({
      ...current,
      [field]: event.target.value,
    }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    createAuthSession();
    navigate("/chat", { replace: true });
  };

  return (
    <AuthShell accent="right">
      <div className="mx-auto max-w-[38rem] rounded-[2rem] border border-border-subtle bg-white px-6 py-8 shadow-soft sm:px-10 md:px-14 md:py-10">
        <div className="flex flex-col items-center text-center">
          <LogoMark className="size-15 rounded-2xl" />
          <h1 className="mt-10 text-5xl font-semibold tracking-[-0.04em] text-text-primary sm:text-6xl">
            Tạo tài khoản mới
          </h1>
          <p className="mt-4 text-[1.7rem] text-text-secondary">Bắt đầu hành trình với trợ lý AI thông minh</p>
        </div>

        <form onSubmit={handleSubmit} className="mt-10 space-y-7">
          <AuthInput
            label="Họ và tên"
            value={form.fullName}
            onChange={handleChange("fullName")}
            placeholder="Nguyễn Văn A"
            required
          />
          <AuthInput
            label="Địa chỉ Email"
            type="email"
            value={form.email}
            onChange={handleChange("email")}
            placeholder="example@domain.com"
            required
          />
          <div className="grid gap-5 sm:grid-cols-2">
            <AuthInput
              label="Mật khẩu"
              type="password"
              value={form.password}
              onChange={handleChange("password")}
              placeholder="••••••••"
              required
            />
            <AuthInput
              label="Xác nhận"
              type="password"
              value={form.confirmPassword}
              onChange={handleChange("confirmPassword")}
              placeholder="••••••••"
              required
            />
          </div>

          <button
            type="submit"
            className="flex h-15 w-full items-center justify-center gap-3 rounded-2xl bg-teal px-6 text-2xl font-semibold text-white transition hover:bg-teal-strong"
          >
            <span>Tạo tài khoản</span>
            <ArrowRight className="size-6" />
          </button>

          <div className="flex items-center gap-4 text-base text-text-secondary">
            <div className="h-px flex-1 bg-border-subtle" />
            <span>Hoặc đăng ký bằng</span>
            <div className="h-px flex-1 bg-border-subtle" />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <SocialButton icon="◉">Google</SocialButton>
            <SocialButton icon="◔">GitHub</SocialButton>
          </div>
        </form>

        <div className="mt-10 border-t border-border-subtle pt-8 text-center">
          <p className="text-[1.8rem] text-text-secondary">
            Đã có tài khoản?{" "}
            <Link to="/login" className="font-semibold text-teal transition hover:text-teal-strong">
              Đăng nhập ngay
            </Link>
          </p>
          <p className="mx-auto mt-8 max-w-lg text-sm leading-7 text-text-muted">
            Bằng cách đăng ký, bạn đồng ý với Điều khoản Dịch vụ và Chính sách Bảo mật của chúng tôi.
          </p>
        </div>
      </div>
    </AuthShell>
  );
}

export default RegisterPage;
