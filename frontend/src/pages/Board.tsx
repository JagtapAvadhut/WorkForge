import { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import { BoardColumn } from '@/components/board/BoardColumn';
import { BoardCard } from '@/components/board/BoardCard';
import { FullPageSpinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { useToast } from '@/components/ui/Toast';
import { boardApi, type BoardData } from '@/api/boardApi';
import { issueApi } from '@/api/issueApi';
import { useProjectContext } from '@/hooks/useProjectContext';
import { KanbanSquare } from 'lucide-react';
import type { Issue } from '@/types';

export default function Board() {
  const { project } = useProjectContext();
  const projectKey = project.key;
  const queryClient = useQueryClient();
  const toast = useToast();
  const [activeIssue, setActiveIssue] = useState<Issue | null>(null);

  const queryKey = ['board', projectKey];
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey,
    queryFn: () => boardApi.get(projectKey),
  });

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));

  const columnIssues = useMemo(() => {
    const map = new Map<string, Issue[]>();
    if (!data) return map;
    for (const col of data.board.columns) map.set(col.id, []);
    for (const issue of data.issues) {
      const col = data.board.columns.find((c) => c.statusIds.includes(issue.status.id));
      if (col) map.get(col.id)!.push(issue);
    }
    return map;
  }, [data]);

  const onDragStart = (event: DragStartEvent) => {
    const issue = event.active.data.current?.issue as Issue | undefined;
    if (issue) setActiveIssue(issue);
  };

  const onDragEnd = async (event: DragEndEvent) => {
    setActiveIssue(null);
    const { active, over } = event;
    if (!over || !data) return;

    const issue = active.data.current?.issue as Issue | undefined;
    const targetColumn = data.board.columns.find((c) => c.id === over.id);
    if (!issue || !targetColumn) return;

    // Already in this column? Nothing to do.
    if (targetColumn.statusIds.includes(issue.status.id)) return;

    const targetStatusId = targetColumn.statusIds[0];
    if (!targetStatusId) return;

    // Optimistic update
    const previous = queryClient.getQueryData<BoardData>(queryKey);
    queryClient.setQueryData<BoardData>(queryKey, (old) => {
      if (!old) return old;
      const newStatus =
        old.issues.find((i) => i.status.id === targetStatusId)?.status ??
        ({ id: targetStatusId, name: targetColumn.name, category: targetColumn.category } as Issue['status']);
      return {
        ...old,
        issues: old.issues.map((i) => (i.id === issue.id ? { ...i, status: newStatus } : i)),
      };
    });

    try {
      await issueApi.transition(issue.key, { statusId: targetStatusId });
      toast.success('Issue moved', `${issue.key} → ${targetColumn.name}`);
      void queryClient.invalidateQueries({ queryKey });
      void queryClient.invalidateQueries({ queryKey: ['issue', issue.key] });
    } catch (err) {
      queryClient.setQueryData(queryKey, previous);
      toast.error('Could not move issue', err instanceof Error ? err.message : undefined);
    }
  };

  if (isLoading) return <FullPageSpinner label="Loading board…" />;
  if (isError || !data) return <ErrorState title="Unable to load board" onRetry={() => refetch()} />;

  if (data.board.columns.length === 0) {
    return (
      <EmptyState
        icon={KanbanSquare}
        title="No board columns configured"
        description="Configure workflow statuses in project settings to use the board."
      />
    );
  }

  return (
    <DndContext sensors={sensors} onDragStart={onDragStart} onDragEnd={onDragEnd}>
      <div className="wf-scrollbar flex gap-4 overflow-x-auto pb-4">
        {data.board.columns.map((column) => (
          <BoardColumn key={column.id} column={column} issues={columnIssues.get(column.id) ?? []} />
        ))}
      </div>
      <DragOverlay>{activeIssue ? <BoardCard issue={activeIssue} overlay /> : null}</DragOverlay>
    </DndContext>
  );
}
