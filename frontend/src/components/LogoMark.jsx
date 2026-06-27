import { Sparkles, Zap } from "lucide-react";

function LogoMark({ variant = "chat", className = "" }) {
  const isAuth = variant === "auth";

  return (
    <div
      className={`flex size-14 items-center justify-center rounded-2xl ${
        isAuth ? "bg-teal text-white" : "bg-hero-mint text-teal"
      } ${className}`}
    >
      {isAuth ? <Sparkles className="size-7" strokeWidth={2.2} /> : <Zap className="size-7" strokeWidth={2.2} />}
    </div>
  );
}

export default LogoMark;
