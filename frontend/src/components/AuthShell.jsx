function AuthShell({ children, accent = "left" }) {
  const accentClass =
    accent === "right"
      ? "bg-[radial-gradient(circle_at_bottom_right,_rgba(216,251,241,0.85),_transparent_18rem)]"
      : "bg-[radial-gradient(circle_at_top_left,_rgba(216,251,241,0.85),_transparent_20rem)]";

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-10 sm:px-6">
      <div className={`pointer-events-none absolute inset-0 ${accentClass}`} />
      <div className="pointer-events-none absolute inset-y-0 right-0 hidden w-80 bg-[radial-gradient(circle_at_center,_rgba(220,226,243,0.85),_transparent_70%)] lg:block" />
      <div className="relative z-10 w-full max-w-[36rem]">{children}</div>
    </div>
  );
}

export default AuthShell;
