import { PRIORITY_META } from '@/utils/issueMeta';
import { cn } from '@/utils/cn';
import type { IssuePriority } from '@/types';

interface IssuePriorityIconProps {
  priority: IssuePriority;
  size?: number;
  withLabel?: boolean;
  className?: string;
}

export function IssuePriorityIcon({ priority, size = 16, withLabel, className }: IssuePriorityIconProps) {
  const meta = PRIORITY_META[priority];
  const Icon = meta.icon;
  return (
    <span className={cn('inline-flex items-center gap-1.5', className)} title={`${meta.label} priority`}>
      <Icon size={size} className={meta.className} aria-hidden />
      {withLabel ? <span className="text-sm text-content">{meta.label}</span> : null}
      <span className="sr-only">{meta.label} priority</span>
    </span>
  );
}
