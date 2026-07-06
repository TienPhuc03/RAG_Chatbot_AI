import { Outlet } from "react-router-dom";
import { useState } from "react";
import AppHeader from "../components/AppHeader";
import Sidebar from "../components/Sidebar";
import { BenchmarkProvider } from "../context/BenchmarkContext";
import { DocumentsProvider } from "../context/DocumentsContext";

function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <BenchmarkProvider>
      <DocumentsProvider>
        <div className="flex h-screen overflow-hidden bg-transparent">
          <Sidebar mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)} />
          <div className="flex h-screen min-w-0 flex-1 flex-col md:ml-[21rem]">
            <AppHeader onOpenSidebar={() => setMobileOpen(true)} />
            <main className="flex min-h-0 flex-1 flex-col overflow-hidden">
              <Outlet />
            </main>
          </div>
        </div>
      </DocumentsProvider>
    </BenchmarkProvider>
  );
}

export default AppLayout;
