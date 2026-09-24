import { useState } from 'react';
import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { Search, ShieldCheck, UserRound } from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { useToast } from '@/components/ui/Toast';
import { userApi } from '@/api/userApi';
import { useDebounce } from '@/hooks/useDebounce';
import { formatDate } from '@/utils/format';
import type { Role, User } from '@/types';

const ROLE_TONE: Record<Role, 'accent' | 'info' | 'neutral' | 'warning'> = {
  ADMIN: 'warning',
  PROJECT_LEAD: 'accent',
  MEMBER: 'info',
  VIEWER: 'neutral',
};

export default function AdminUsers() {
  const [search, setSearch] = useState('');
  const debounced = useDebounce(search.trim(), 300);
  const queryClient = useQueryClient();
  const toast = useToast();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin', 'users', { search: debounced }],
    queryFn: () => userApi.list({ search: debounced || undefined, size: 50 }),
    placeholderData: keepPreviousData,
  });

  const toggleActive = useMutation({
    mutationFn: (user: User) =>
      user.active ? userApi.deactivate(user.id) : userApi.activate(user.id),
    onSuccess: (updated) => {
      toast.success(updated.active ? 'User activated' : 'User deactivated');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (e: unknown) => toast.error('Could not update user', e instanceof Error ? e.message : undefined),
  });

  const users = data?.content ?? [];

  return (
    <div>
      <PageHeader title="Users" description="Manage the people who have access to WorkForge." />

      <div className="relative mb-4 max-w-sm">
        <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-content-subtle" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search users…"
          className="h-9 w-full rounded-md border border-border bg-surface pl-9 pr-3 text-sm text-content placeholder:text-content-subtle focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
        />
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : users.length === 0 ? (
        <EmptyState icon={UserRound} title="No users found" description="Try a different search term." />
      ) : (
        <div className="overflow-hidden rounded-lg border border-border bg-surface">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-content-subtle">
                <th className="px-4 py-3 font-medium">User</th>
                <th className="hidden px-4 py-3 font-medium md:table-cell">Roles</th>
                <th className="hidden px-4 py-3 font-medium lg:table-cell">Joined</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-canvas">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <Avatar user={user} size={32} />
                      <div className="min-w-0">
                        <p className="truncate font-medium text-content">{user.fullName}</p>
                        <p className="truncate text-xs text-content-subtle">{user.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="hidden px-4 py-3 md:table-cell">
                    <div className="flex flex-wrap gap-1">
                      {user.roles.map((r) => (
                        <Badge key={r} tone={ROLE_TONE[r]}>
                          {r === 'ADMIN' ? <ShieldCheck size={11} /> : null}
                          {r}
                        </Badge>
                      ))}
                    </div>
                  </td>
                  <td className="hidden px-4 py-3 text-content-muted lg:table-cell">
                    {formatDate(user.createdAt)}
                  </td>
                  <td className="px-4 py-3">
                    <Badge tone={user.active ? 'success' : 'neutral'}>
                      {user.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button
                      size="sm"
                      variant={user.active ? 'ghost' : 'secondary'}
                      loading={toggleActive.isPending && toggleActive.variables?.id === user.id}
                      onClick={() => toggleActive.mutate(user)}
                    >
                      {user.active ? 'Deactivate' : 'Activate'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
