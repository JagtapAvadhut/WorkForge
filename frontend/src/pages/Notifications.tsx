import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AtSign, Bell, CheckCheck, CircleUser, MessageSquare, RefreshCw } from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { useToast } from '@/components/ui/Toast';
import { notificationApi } from '@/api/notificationApi';
import { formatRelative } from '@/utils/format';
import { cn } from '@/utils/cn';
import type { AppNotification, NotificationType } from '@/types';

const ICONS: Record<NotificationType, typeof Bell> = {
  ASSIGNED: CircleUser,
  MENTIONED: AtSign,
  COMMENTED: MessageSquare,
  STATUS_CHANGED: RefreshCw,
  DUE_SOON: Bell,
};

export default function Notifications() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: () => notificationApi.list({ size: 50 }),
  });

  const markRead = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const markAll = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => {
      toast.success('All notifications marked as read');
      void queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const notifications = data?.content ?? [];
  const hasUnread = notifications.some((n) => !n.read);

  return (
    <div>
      <PageHeader
        title="Notifications"
        description="Stay on top of mentions, assignments, and updates."
        actions={
          hasUnread ? (
            <Button variant="secondary" onClick={() => markAll.mutate()} loading={markAll.isPending}>
              <CheckCheck size={16} />
              Mark all read
            </Button>
          ) : null
        }
      />

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : notifications.length === 0 ? (
        <EmptyState icon={Bell} title="You're all caught up" description="New notifications will appear here." />
      ) : (
        <ul className="divide-y divide-border overflow-hidden rounded-lg border border-border bg-surface">
          {notifications.map((n) => (
            <NotificationRow key={n.id} n={n} onRead={() => markRead.mutate(n.id)} />
          ))}
        </ul>
      )}
    </div>
  );
}

function NotificationRow({ n, onRead }: { n: AppNotification; onRead: () => void }) {
  const Icon = ICONS[n.type] ?? Bell;
  const content = (
    <div className={cn('flex items-start gap-3 px-4 py-3 transition-colors hover:bg-canvas', !n.read && 'bg-accent/5')}>
      <span
        className={cn(
          'mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full',
          n.read ? 'bg-canvas text-content-subtle' : 'bg-accent/10 text-accent',
        )}
      >
        <Icon size={17} />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-content">{n.title}</p>
        {n.body ? <p className="mt-0.5 text-sm text-content-muted">{n.body}</p> : null}
        <p className="mt-1 text-xs text-content-subtle">{formatRelative(n.createdAt)}</p>
      </div>
      {!n.read ? <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-accent" aria-label="Unread" /> : null}
    </div>
  );

  return (
    <li onClick={onRead}>
      {n.issueKey ? <Link to={`/issues/${n.issueKey}`}>{content}</Link> : content}
    </li>
  );
}
