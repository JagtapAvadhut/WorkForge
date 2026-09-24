import { useNavigate } from 'react-router-dom';
import { useDraggable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import { MessageSquare } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { IssueTypeIcon } from '@/components/issues/IssueTypeIcon';
import { IssuePriorityIcon } from '@/components/issues/IssuePriorityIcon';
import { cn } from '@/utils/cn';
import type { Issue } from '@/types';

interface BoardCardProps {
  issue: Issue;
  overlay?: boolean;
}

export function BoardCard({ issue, overlay }: BoardCardProps) {
  const navigate = useNavigate();
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: issue.id,
    data: { issue },
  });

  const style = transform ? { transform: CSS.Translate.toString(transform) } : undefined;

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      onClick={() => !overlay && navigate(`/issues/${issue.key}`)}
      className={cn(
        'group cursor-grab rounded-md border border-border bg-surface p-3 shadow-card transition-shadow active:cursor-grabbing',
        'hover:border-border-strong hover:shadow-raised',
        isDragging && !overlay && 'opacity-40',
        overlay && 'rotate-2 cursor-grabbing shadow-popover ring-1 ring-accent/40',
      )}
    >
      <p className="mb-2 line-clamp-3 text-sm text-content">{issue.summary}</p>
      {issue.labels?.length ? (
        <div className="mb-2 flex flex-wrap gap-1">
          {issue.labels.slice(0, 3).map((l) => (
            <Badge key={l.id} tone="neutral">
              {l.name}
            </Badge>
          ))}
        </div>
      ) : null}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <IssueTypeIcon type={issue.type} size={15} />
          <span className="font-mono text-xs text-content-subtle">{issue.key}</span>
          <IssuePriorityIcon priority={issue.priority} size={15} />
        </div>
        <div className="flex items-center gap-2">
          {issue.commentCount ? (
            <span className="flex items-center gap-1 text-xs text-content-subtle">
              <MessageSquare size={13} />
              {issue.commentCount}
            </span>
          ) : null}
          {typeof issue.storyPoints === 'number' ? (
            <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-canvas px-1.5 text-xs font-medium text-content-muted ring-1 ring-inset ring-border">
              {issue.storyPoints}
            </span>
          ) : null}
          <Avatar user={issue.assignee} size={22} />
        </div>
      </div>
    </div>
  );
}
