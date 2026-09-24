import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import type { Role } from '@/types';
import { EmptyState } from '@/components/ui/EmptyState';
import { ShieldAlert } from 'lucide-react';

interface RequireRoleProps {
  roles: Role[];
}

/** Frontend guard — hides admin routes. Backend still enforces authorization. */
export function RequireRole({ roles }: RequireRoleProps) {
  const user = useAuthStore((s) => s.user);
  const allowed = user?.roles?.some((r) => roles.includes(r));

  if (!user) return <Navigate to="/login" replace />;

  if (!allowed) {
    return (
      <div className="py-16">
        <EmptyState
          icon={ShieldAlert}
          title="Access restricted"
          description="You don't have permission to view this area. Contact an administrator if you believe this is a mistake."
        />
      </div>
    );
  }

  return <Outlet />;
}
