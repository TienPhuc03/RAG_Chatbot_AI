function SocialButton({ icon, children }) {
  return (
    <button
      type="button"
      className="flex h-14 items-center justify-center gap-3 rounded-2xl border border-border-subtle bg-surface px-4 text-lg font-medium text-text-primary transition hover:border-teal-strong/30 hover:bg-surface-muted"
    >
      <span className="text-xl">{icon}</span>
      <span>{children}</span>
    </button>
  );
}

export default SocialButton;
