function SectionIntro({ eyebrow, title, description, action }) {
  return (
    <div className="rounded-[2rem] border border-border-subtle bg-white p-8 shadow-soft">
      <div className="text-sm font-semibold uppercase tracking-[0.18em] text-text-muted">{eyebrow}</div>
      <div className="mt-4 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-3">
          <h2 className="text-4xl font-semibold tracking-[-0.03em] text-text-primary">{title}</h2>
          <p className="max-w-2xl text-lg leading-8 text-text-secondary">{description}</p>
        </div>
        {action}
      </div>
    </div>
  );
}

export default SectionIntro;
