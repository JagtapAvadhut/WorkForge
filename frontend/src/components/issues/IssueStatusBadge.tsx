import { STATUS_CATEGORY_META } from '@/utils/issueMeta';
import { cn } from '@/utils/cn';
import type { IssueStatus } from '@/types';

interface IssueStatusBadgeProps {
  status: IssueStatus;
  className?: string;
}

export function IssueStatusBadge({ status, className }: IssueStatusBadgeProps) {
  const meta = STATUS_CATEGORY_META[status.category];
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded px-2 py-0.5 text-xs font-semibold uppercase tracking-wide',
        meta.badge,
        className,
      )}
    >
      <span className={cn('h-1.5 w-1.5 rounded-full', meta.dot)} aria-hidden />
      {status.name}
    </span>
  );
}
