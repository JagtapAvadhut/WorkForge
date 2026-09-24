import { Outlet, Navigate, useLocation } from 'react-router-dom';
import { Logo } from '@/components/common/Logo';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { useAuthStore } from '@/stores/authStore';

export function AuthLayout() {
  const isAuthenticated = useAuthStore((s) => Boolean(s.accessToken));
  const location = useLocation();

  if (isAuthenticated) {
    const params = new URLSearchParams(location.search);
    return <Navigate to={params.get('redirect') || '/dashboard'} replace />;
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-10">
      {/* Atmospheric background — layered gradients, not flat white. */}
      <div
        className="pointer-events-none absolute inset-0 -z-10"
        style={{
          background:
            'radial-gradient(60rem 40rem at 110% -10%, rgb(var(--wf-accent) / 0.10), transparent 55%), radial-gradient(50rem 35rem at -10% 110%, rgb(var(--wf-info) / 0.08), transparent 55%)',
        }}
        aria-hidden
      />
      <div className="absolute right-4 top-4">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-md">
        <div className="mb-8 flex flex-col items-center text-center">
          <Logo size={40} />
          <p className="mt-3 max-w-xs text-sm text-content-muted">
            Enterprise project management and issue tracking for high-performing teams.
          </p>
        </div>
        <div className="rounded-xl border border-border bg-surface/80 p-6 shadow-raised backdrop-blur sm:p-8">
          <Outlet />
        </div>
        <p className="mt-6 text-center text-xs text-content-subtle">
          © {new Date().getFullYear()} WorkForge. All rights reserved.
        </p>
      </div>
    </div>
  );
}
