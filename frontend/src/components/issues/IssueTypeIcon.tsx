import { ISSUE_TYPE_META } from '@/utils/issueMeta';
import { cn } from '@/utils/cn';
import type { IssueType } from '@/types';

interface IssueTypeIconProps {
  type: IssueType;
  size?: number;
  withLabel?: boolean;
  className?: string;
}

export function IssueTypeIcon({ type, size = 16, withLabel, className }: IssueTypeIconProps) {
  const meta = ISSUE_TYPE_META[type];
  const Icon = meta.icon;
  return (
    <span className={cn('inline-flex items-center gap-1.5', className)} title={meta.label}>
      <Icon size={size} className={meta.className} aria-hidden />
      {withLabel ? <span className="text-sm text-content">{meta.label}</span> : null}
      <span className="sr-only">{meta.label}</span>
    </span>
  );
}
