function AuthInput({ label, action, ...props }) {
  return (
    <label className="block space-y-3 text-sm font-medium text-text-primary">
      <span className="flex items-center justify-between gap-4">
        <span>{label}</span>
        {action}
      </span>
      <input
        className="h-15 w-full rounded-2xl border border-border-subtle bg-surface px-5 text-lg text-text-primary outline-none transition focus:border-teal-strong focus:ring-4 focus:ring-teal-strong/10"
        {...props}
      />
    </label>
  );
}

export default AuthInput;
