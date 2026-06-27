import { Outlet } from "react-router-dom";
import { useState } from "react";
import AppHeader from "../components/AppHeader";
import Sidebar from "../components/Sidebar";

function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="flex min-h-screen bg-transparent">
      <Sidebar mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)} />
      <div className="flex min-h-screen min-w-0 flex-1 flex-col">
        <AppHeader onOpenSidebar={() => setMobileOpen(true)} />
        <main className="flex min-h-0 flex-1 flex-col">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default AppLayout;
