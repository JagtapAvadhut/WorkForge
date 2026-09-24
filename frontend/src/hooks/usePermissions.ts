import { useAuthStore } from '@/stores/authStore';
import type { Role } from '@/types';

/**
 * Frontend permission helpers used only to hide/disable UI affordances.
 * The backend remains the source of truth for authorization.
 */
export function usePermissions() {
  const user = useAuthStore((s) => s.user);
  const roles = user?.roles ?? [];

  const hasRole = (...required: Role[]) => required.some((r) => roles.includes(r));

  return {
    roles,
    isAdmin: hasRole('ADMIN'),
    isProjectLead: hasRole('ADMIN', 'PROJECT_LEAD'),
    canManageProjects: hasRole('ADMIN', 'PROJECT_LEAD'),
    canManageUsers: hasRole('ADMIN'),
    canEditIssues: hasRole('ADMIN', 'PROJECT_LEAD', 'MEMBER'),
    canComment: hasRole('ADMIN', 'PROJECT_LEAD', 'MEMBER'),
    hasRole,
  };
}
