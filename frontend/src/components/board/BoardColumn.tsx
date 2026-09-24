import { useDroppable } from '@dnd-kit/core';
import { BoardCard } from './BoardCard';
import { cn } from '@/utils/cn';
import type { BoardColumnDef, Issue } from '@/types';

interface BoardColumnProps {
  column: BoardColumnDef;
  issues: Issue[];
}

export function BoardColumn({ column, issues }: BoardColumnProps) {
  const { setNodeRef, isOver } = useDroppable({ id: column.id, data: { column } });

  const overLimit = column.wipLimit != null && issues.length > column.wipLimit;

  return (
    <div className="flex w-72 shrink-0 flex-col">
      <div className="mb-2 flex items-center justify-between px-1">
        <div className="flex items-center gap-2">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-content-muted">
            {column.name}
          </h3>
          <span className="rounded-full bg-canvas px-1.5 py-0.5 text-xs font-medium text-content-subtle ring-1 ring-inset ring-border">
            {issues.length}
          </span>
        </div>
        {column.wipLimit != null ? (
          <span className={cn('text-xs', overLimit ? 'font-semibold text-danger' : 'text-content-subtle')}>
            WIP {issues.length}/{column.wipLimit}
          </span>
        ) : null}
      </div>
      <div
        ref={setNodeRef}
        className={cn(
          'wf-scrollbar flex max-h-[calc(100vh-14rem)] min-h-24 flex-1 flex-col gap-2 overflow-y-auto rounded-lg border border-dashed border-border bg-canvas/40 p-2 transition-colors',
          isOver && 'border-accent/60 bg-accent/5',
        )}
      >
        {issues.map((issue) => (
          <BoardCard key={issue.id} issue={issue} />
        ))}
        {issues.length === 0 ? (
          <p className="px-2 py-6 text-center text-xs text-content-subtle">No issues</p>
        ) : null}
      </div>
    </div>
  );
}
