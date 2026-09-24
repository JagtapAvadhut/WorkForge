import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { CheckCircle2, ChevronDown, MoreHorizontal, Play, Plus } from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input, Textarea } from '@/components/ui/Input';
import { FullPageSpinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DropdownMenu, MenuItem, MenuSeparator } from '@/components/ui/DropdownMenu';
import { useToast } from '@/components/ui/Toast';
import { IssueTypeIcon } from '@/components/issues/IssueTypeIcon';
import { IssuePriorityIcon } from '@/components/issues/IssuePriorityIcon';
import { Avatar } from '@/components/ui/Avatar';
import { Link } from 'react-router-dom';
import { sprintApi, type CreateSprintPayload } from '@/api/sprintApi';
import { useProjectContext } from '@/hooks/useProjectContext';
import { usePermissions } from '@/hooks/usePermissions';
import { useUiStore } from '@/stores/uiStore';
import { createSprintSchema, type CreateSprintFormValues } from '@/utils/schemas';
import { formatDate, pluralize } from '@/utils/format';
import type { Issue, Sprint } from '@/types';

export default function Backlog() {
  const { project } = useProjectContext();
  const projectKey = project.key;
  const queryClient = useQueryClient();
  const toast = useToast();
  const { isProjectLead } = usePermissions();
  const openCreateIssue = useUiStore((s) => s.openCreateIssue);
  const [createSprintOpen, setCreateSprintOpen] = useState(false);
  const [completing, setCompleting] = useState<Sprint | null>(null);

  const queryKey = ['backlog', projectKey];
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey,
    queryFn: () => sprintApi.backlog(projectKey),
  });

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey });
    void queryClient.invalidateQueries({ queryKey: ['board', projectKey] });
  };

  const startMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.start(sprintId),
    onSuccess: () => {
      toast.success('Sprint started');
      invalidate();
    },
    onError: (e: unknown) => toast.error('Could not start sprint', e instanceof Error ? e.message : undefined),
  });

  const completeMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.complete(sprintId),
    onSuccess: () => {
      toast.success('Sprint completed');
      setCompleting(null);
      invalidate();
    },
    onError: (e: unknown) => toast.error('Could not complete sprint', e instanceof Error ? e.message : undefined),
  });

  const moveMutation = useMutation({
    mutationFn: ({ issueKey, sprintId }: { issueKey: string; sprintId: string | null }) =>
      sprintApi.moveIssue(issueKey, sprintId),
    onSuccess: () => invalidate(),
    onError: (e: unknown) => toast.error('Could not move issue', e instanceof Error ? e.message : undefined),
  });

  if (isLoading) return <FullPageSpinner label="Loading backlog…" />;
  if (isError || !data) return <ErrorState title="Unable to load backlog" onRetry={() => refetch()} />;

  const sprintTargets = data.sprints.map((s) => s.sprint);

  return (
    <div>
      <PageHeader
        title="Backlog"
        description="Plan upcoming work by organising issues into sprints."
        actions={
          isProjectLead ? (
            <Button variant="secondary" onClick={() => setCreateSprintOpen(true)}>
              <Plus size={16} />
              Create sprint
            </Button>
          ) : null
        }
      />

      <div className="space-y-4">
        {data.sprints.map(({ sprint, issues }) => (
          <SprintSection
            key={sprint.id}
            sprint={sprint}
            issues={issues}
            sprintTargets={sprintTargets}
            canManage={isProjectLead}
            onStart={() => startMutation.mutate(sprint.id)}
            starting={startMutation.isPending}
            onComplete={() => setCompleting(sprint)}
            onMove={(issueKey, sprintId) => moveMutation.mutate({ issueKey, sprintId })}
            onCreateIssue={() => openCreateIssue({ projectKey, sprintId: sprint.id })}
          />
        ))}

        <BacklogSection
          issues={data.backlog}
          sprintTargets={sprintTargets}
          onMove={(issueKey, sprintId) => moveMutation.mutate({ issueKey, sprintId })}
          onCreateIssue={() => openCreateIssue({ projectKey })}
        />
      </div>

      <CreateSprintModal
        open={createSprintOpen}
        onClose={() => setCreateSprintOpen(false)}
        projectKey={projectKey}
        onCreated={invalidate}
      />

      <ConfirmDialog
        open={Boolean(completing)}
        title={`Complete ${completing?.name ?? 'sprint'}?`}
        description="Completed issues will be marked done. Incomplete issues move back to the backlog."
        confirmLabel="Complete sprint"
        loading={completeMutation.isPending}
        onCancel={() => setCompleting(null)}
        onConfirm={() => completing && completeMutation.mutate(completing.id)}
      />
    </div>
  );
}

function SprintSection({
  sprint,
  issues,
  sprintTargets,
  canManage,
  onStart,
  starting,
  onComplete,
  onMove,
  onCreateIssue,
}: {
  sprint: Sprint;
  issues: Issue[];
  sprintTargets: Sprint[];
  canManage: boolean;
  onStart: () => void;
  starting: boolean;
  onComplete: () => void;
  onMove: (issueKey: string, sprintId: string | null) => void;
  onCreateIssue: () => void;
}) {
  const [open, setOpen] = useState(true);
  const points = issues.reduce((sum, i) => sum + (i.storyPoints ?? 0), 0);

  return (
    <section className="rounded-lg border border-border bg-surface shadow-card">
      <header className="flex items-center justify-between gap-2 border-b border-border px-4 py-3">
        <button className="flex min-w-0 items-center gap-2 text-left" onClick={() => setOpen((o) => !o)}>
          <ChevronDown size={16} className={`shrink-0 text-content-subtle transition-transform ${open ? '' : '-rotate-90'}`} />
          <span className="truncate font-semibold text-content">{sprint.name}</span>
          {sprint.state === 'ACTIVE' ? <Badge tone="success">Active</Badge> : null}
          {sprint.state === 'COMPLETED' ? <Badge tone="neutral">Completed</Badge> : null}
          <span className="hidden text-xs text-content-subtle sm:inline">
            {sprint.startDate ? `${formatDate(sprint.startDate)} – ${formatDate(sprint.endDate)}` : 'Not scheduled'}
          </span>
        </button>
        <div className="flex shrink-0 items-center gap-2">
          <span className="hidden text-xs text-content-subtle sm:inline">
            {pluralize(issues.length, 'issue')} · {points} pts
          </span>
          {canManage && sprint.state === 'FUTURE' ? (
            <Button size="sm" variant="secondary" onClick={onStart} loading={starting} disabled={issues.length === 0}>
              <Play size={14} />
              Start
            </Button>
          ) : null}
          {canManage && sprint.state === 'ACTIVE' ? (
            <Button size="sm" variant="secondary" onClick={onComplete}>
              <CheckCircle2 size={14} />
              Complete
            </Button>
          ) : null}
        </div>
      </header>

      {open ? (
        <div className="divide-y divide-border">
          {issues.length === 0 ? (
            <p className="px-4 py-6 text-center text-sm text-content-subtle">
              No issues in this sprint yet.
            </p>
          ) : (
            issues.map((issue) => (
              <BacklogRow
                key={issue.id}
                issue={issue}
                sprintTargets={sprintTargets}
                currentSprintId={sprint.id}
                onMove={onMove}
              />
            ))
          )}
          <button
            onClick={onCreateIssue}
            className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-content-subtle hover:bg-canvas hover:text-content"
          >
            <Plus size={15} /> Add issue to {sprint.name}
          </button>
        </div>
      ) : null}
    </section>
  );
}

function BacklogSection({
  issues,
  sprintTargets,
  onMove,
  onCreateIssue,
}: {
  issues: Issue[];
  sprintTargets: Sprint[];
  onMove: (issueKey: string, sprintId: string | null) => void;
  onCreateIssue: () => void;
}) {
  const points = issues.reduce((sum, i) => sum + (i.storyPoints ?? 0), 0);
  return (
    <section className="rounded-lg border border-border bg-surface shadow-card">
      <header className="flex items-center justify-between border-b border-border px-4 py-3">
        <h2 className="font-semibold text-content">Backlog</h2>
        <span className="text-xs text-content-subtle">
          {pluralize(issues.length, 'issue')} · {points} pts
        </span>
      </header>
      <div className="divide-y divide-border">
        {issues.length === 0 ? (
          <div className="p-4">
            <EmptyState title="Backlog is empty" description="Create issues to start planning." />
          </div>
        ) : (
          issues.map((issue) => (
            <BacklogRow key={issue.id} issue={issue} sprintTargets={sprintTargets} currentSprintId={null} onMove={onMove} />
          ))
        )}
        <button
          onClick={onCreateIssue}
          className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-content-subtle hover:bg-canvas hover:text-content"
        >
          <Plus size={15} /> Add issue to backlog
        </button>
      </div>
    </section>
  );
}

function BacklogRow({
  issue,
  sprintTargets,
  currentSprintId,
  onMove,
}: {
  issue: Issue;
  sprintTargets: Sprint[];
  currentSprintId: string | null;
  onMove: (issueKey: string, sprintId: string | null) => void;
}) {
  return (
    <div className="flex items-center gap-3 px-4 py-2.5 hover:bg-canvas">
      <IssueTypeIcon type={issue.type} />
      <Link to={`/issues/${issue.key}`} className="font-mono text-xs text-content-subtle hover:text-accent">
        {issue.key}
      </Link>
      <Link to={`/issues/${issue.key}`} className="min-w-0 flex-1 truncate text-sm text-content hover:text-accent">
        {issue.summary}
      </Link>
      {typeof issue.storyPoints === 'number' ? (
        <span className="hidden h-5 min-w-5 items-center justify-center rounded-full bg-canvas px-1.5 text-xs font-medium text-content-muted ring-1 ring-inset ring-border sm:inline-flex">
          {issue.storyPoints}
        </span>
      ) : null}
      <IssuePriorityIcon priority={issue.priority} />
      <Avatar user={issue.assignee} size={22} />
      <DropdownMenu
        trigger={
          <span className="flex h-7 w-7 items-center justify-center rounded-md text-content-subtle hover:bg-surface hover:text-content">
            <MoreHorizontal size={16} />
          </span>
        }
      >
        {(close) => (
          <>
            <p className="px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-content-subtle">
              Move to
            </p>
            {currentSprintId !== null ? (
              <MenuItem
                onClick={() => {
                  onMove(issue.key, null);
                  close();
                }}
              >
                Backlog
              </MenuItem>
            ) : null}
            {sprintTargets
              .filter((s) => s.id !== currentSprintId && s.state !== 'COMPLETED')
              .map((s) => (
                <MenuItem
                  key={s.id}
                  onClick={() => {
                    onMove(issue.key, s.id);
                    close();
                  }}
                >
                  {s.name}
                </MenuItem>
              ))}
            <MenuSeparator />
            <MenuItem>
              <Link to={`/issues/${issue.key}`}>Open issue</Link>
            </MenuItem>
          </>
        )}
      </DropdownMenu>
    </div>
  );
}

function CreateSprintModal({
  open,
  onClose,
  projectKey,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  projectKey: string;
  onCreated: () => void;
}) {
  const toast = useToast();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateSprintFormValues>({ resolver: zodResolver(createSprintSchema) });

  const mutation = useMutation({
    mutationFn: (payload: CreateSprintPayload) => sprintApi.create(projectKey, payload),
    onSuccess: () => {
      toast.success('Sprint created');
      reset();
      onCreated();
      onClose();
    },
    onError: (e: unknown) => toast.error('Could not create sprint', e instanceof Error ? e.message : undefined),
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create sprint"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="create-sprint-form" loading={mutation.isPending}>
            Create sprint
          </Button>
        </>
      }
    >
      <form
        id="create-sprint-form"
        className="space-y-4"
        onSubmit={handleSubmit((v) =>
          mutation.mutate({
            name: v.name,
            goal: v.goal || undefined,
            startDate: v.startDate || undefined,
            endDate: v.endDate || undefined,
          }),
        )}
      >
        <Input label="Sprint name" placeholder="Sprint 1" error={errors.name?.message} {...register('name')} />
        <Textarea label="Sprint goal" placeholder="What do we want to achieve?" error={errors.goal?.message} {...register('goal')} rows={2} />
        <div className="grid grid-cols-2 gap-4">
          <Input label="Start date" type="date" {...register('startDate')} />
          <Input label="End date" type="date" {...register('endDate')} />
        </div>
      </form>
    </Modal>
  );
}
