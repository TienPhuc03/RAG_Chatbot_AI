import { Bell, Globe2, ShieldCheck, SlidersHorizontal } from "lucide-react";
import SectionIntro from "../components/SectionIntro";

const settingsGroups = [
  {
    icon: Globe2,
    title: "Mặc định workspace",
    description: "Thiết lập ngôn ngữ, phong cách trả lời và chế độ bám sát tài liệu.",
  },
  {
    icon: Bell,
    title: "Thông báo",
    description: "Chọn cách nhận thông báo khi upload hoặc benchmark hoàn tất.",
  },
  {
    icon: ShieldCheck,
    title: "Quyền riêng tư",
    description: "Xem lại lưu phiên, lưu tin nhắn và các thiết lập nhạy cảm về dữ liệu.",
  },
];

function SettingsPage() {
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <SectionIntro
        eyebrow="Cấu hình"
        title="Cài đặt"
        description="Khu vực điều khiển cho workspace trợ lý. Trang này đã sẵn sàng để tích hợp backend về sau."
        action={
          <button className="inline-flex h-13 items-center gap-3 rounded-2xl border border-border-subtle bg-white px-5 text-base font-semibold text-text-primary transition hover:bg-surface-muted">
            <SlidersHorizontal className="size-5" />
            <span>Xem tùy chọn</span>
          </button>
        }
      />

      <div className="mt-8 space-y-5">
        {settingsGroups.map(({ icon: Icon, title, description }) => (
          <div key={title} className="rounded-[2rem] border border-border-subtle bg-white p-6 shadow-soft">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex gap-4">
                <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-surface-soft text-text-primary">
                  <Icon className="size-5" />
                </div>
                <div>
                  <h3 className="text-2xl font-semibold tracking-[-0.03em] text-text-primary">{title}</h3>
                  <p className="mt-3 max-w-3xl text-base leading-7 text-text-secondary">{description}</p>
                </div>
              </div>
              <button className="h-12 rounded-2xl bg-teal px-5 text-sm font-semibold text-white transition hover:bg-teal-strong">
                Cấu hình
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default SettingsPage;
