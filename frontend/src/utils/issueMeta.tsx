import {
  Bookmark,
  Bug,
  CheckSquare,
  ChevronsUp,
  ChevronUp,
  Equal,
  ChevronDown,
  ChevronsDown,
  Layers,
  GitBranch,
  type LucideIcon,
} from 'lucide-react';
import type { IssuePriority, IssueStatusCategory, IssueType } from '@/types';

interface TypeMeta {
  label: string;
  icon: LucideIcon;
  className: string;
}

export const ISSUE_TYPE_META: Record<IssueType, TypeMeta> = {
  STORY: { label: 'Story', icon: Bookmark, className: 'text-success' },
  TASK: { label: 'Task', icon: CheckSquare, className: 'text-info' },
  BUG: { label: 'Bug', icon: Bug, className: 'text-danger' },
  EPIC: { label: 'Epic', icon: Layers, className: 'text-accent' },
  SUBTASK: { label: 'Subtask', icon: GitBranch, className: 'text-content-subtle' },
};

interface PriorityMeta {
  label: string;
  icon: LucideIcon;
  className: string;
}

export const PRIORITY_META: Record<IssuePriority, PriorityMeta> = {
  HIGHEST: { label: 'Highest', icon: ChevronsUp, className: 'text-danger' },
  HIGH: { label: 'High', icon: ChevronUp, className: 'text-danger/80' },
  MEDIUM: { label: 'Medium', icon: Equal, className: 'text-warning' },
  LOW: { label: 'Low', icon: ChevronDown, className: 'text-info' },
  LOWEST: { label: 'Lowest', icon: ChevronsDown, className: 'text-content-subtle' },
};

export const STATUS_CATEGORY_META: Record<
  IssueStatusCategory,
  { label: string; badge: string; dot: string }
> = {
  TODO: {
    label: 'To Do',
    badge: 'bg-slate-500/10 text-slate-600 dark:text-slate-300 ring-1 ring-inset ring-slate-500/20',
    dot: 'bg-slate-400',
  },
  IN_PROGRESS: {
    label: 'In Progress',
    badge: 'bg-info/10 text-info ring-1 ring-inset ring-info/25',
    dot: 'bg-info',
  },
  DONE: {
    label: 'Done',
    badge: 'bg-success/10 text-success ring-1 ring-inset ring-success/25',
    dot: 'bg-success',
  },
};

export const ISSUE_TYPES: IssueType[] = ['STORY', 'TASK', 'BUG', 'EPIC', 'SUBTASK'];
export const ISSUE_PRIORITIES: IssuePriority[] = ['HIGHEST', 'HIGH', 'MEDIUM', 'LOW', 'LOWEST'];
