import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { notificationApi } from '@/api/notificationApi';

export function NotificationBell() {
  const { data } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => notificationApi.unreadCount(),
    refetchInterval: 60_000,
    // Don't blow up the shell if the endpoint isn't ready.
    retry: false,
  });

  const count = data?.count ?? 0;

  return (
    <Link
      to="/notifications"
      className="relative flex h-9 w-9 items-center justify-center rounded-md text-content-muted transition-colors hover:bg-surface hover:text-content"
      aria-label={`Notifications${count ? `, ${count} unread` : ''}`}
    >
      <Bell size={18} />
      {count > 0 ? (
        <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[10px] font-semibold text-white ring-2 ring-canvas">
          {count > 99 ? '99+' : count}
        </span>
      ) : null}
    </Link>
  );
}
