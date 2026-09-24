import { Link } from 'react-router-dom';
import { MessageSquare } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { IssueTypeIcon } from './IssueTypeIcon';
import { IssuePriorityIcon } from './IssuePriorityIcon';
import { IssueStatusBadge } from './IssueStatusBadge';
import { cn } from '@/utils/cn';
import type { Issue } from '@/types';

interface IssueCardProps {
  issue: Issue;
  className?: string;
  showStatus?: boolean;
}

/** A horizontal issue row used in lists (backlog, my work, issues page). */
export function IssueCard({ issue, className, showStatus = true }: IssueCardProps) {
  return (
    <Link
      to={`/issues/${issue.key}`}
      className={cn(
        'group flex items-center gap-3 rounded-md border border-border bg-surface px-3 py-2.5 transition-colors hover:border-border-strong hover:bg-canvas',
        className,
      )}
    >
      <IssueTypeIcon type={issue.type} />
      <span className="font-mono text-xs text-content-subtle">{issue.key}</span>
      <span className="min-w-0 flex-1 truncate text-sm text-content group-hover:text-content">
        {issue.summary}
      </span>
      <div className="flex items-center gap-2.5">
        {issue.labels?.slice(0, 2).map((label) => (
          <Badge key={label.id} tone="neutral" className="hidden lg:inline-flex">
            {label.name}
          </Badge>
        ))}
        {typeof issue.storyPoints === 'number' ? (
          <span className="hidden h-5 min-w-5 items-center justify-center rounded-full bg-canvas px-1.5 text-xs font-medium text-content-muted ring-1 ring-inset ring-border sm:inline-flex">
            {issue.storyPoints}
          </span>
        ) : null}
        {issue.commentCount ? (
          <span className="hidden items-center gap-1 text-xs text-content-subtle sm:flex">
            <MessageSquare size={13} />
            {issue.commentCount}
          </span>
        ) : null}
        <IssuePriorityIcon priority={issue.priority} />
        {showStatus ? <IssueStatusBadge status={issue.status} className="hidden md:inline-flex" /> : null}
        <Avatar user={issue.assignee} size={24} />
      </div>
    </Link>
  );
}
