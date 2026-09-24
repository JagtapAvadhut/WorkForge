import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ChevronRight, History, MessageSquare, Send } from 'lucide-react';
import { FullPageSpinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { Button } from '@/components/ui/Button';
import { Textarea } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Avatar } from '@/components/ui/Avatar';
import { useToast } from '@/components/ui/Toast';
import { IssueTypeIcon } from '@/components/issues/IssueTypeIcon';
import { IssuePriorityIcon } from '@/components/issues/IssuePriorityIcon';
import { IssueStatusBadge } from '@/components/issues/IssueStatusBadge';
import { issueApi } from '@/api/issueApi';
import { commentApi } from '@/api/commentApi';
import { projectApi } from '@/api/projectApi';
import { useProjectStatuses } from '@/hooks/useReferenceData';
import { usePermissions } from '@/hooks/usePermissions';
import { commentSchema, type CommentFormValues } from '@/utils/schemas';
import { formatDateTime, formatRelative } from '@/utils/format';
import { cn } from '@/utils/cn';
import type { ActivityEntry } from '@/types';

type Tab = 'comments' | 'history';

export default function IssueDetail() {
  const { issueKey = '' } = useParams();
  const [tab, setTab] = useState<Tab>('comments');
  const queryClient = useQueryClient();
  const toast = useToast();
  const { canEditIssues, canComment } = usePermissions();

  const issueQuery = useQuery({
    queryKey: ['issue', issueKey],
    queryFn: () => issueApi.get(issueKey),
    enabled: Boolean(issueKey),
  });
  const issue = issueQuery.data;

  const { data: statuses } = useProjectStatuses(issue?.projectKey);
  const { data: members } = useQuery({
    queryKey: ['project', issue?.projectKey, 'members'],
    queryFn: () => projectApi.members(issue!.projectKey),
    enabled: Boolean(issue?.projectKey),
  });

  const invalidateIssue = () => {
    void queryClient.invalidateQueries({ queryKey: ['issue', issueKey] });
    void queryClient.invalidateQueries({ queryKey: ['issue', issueKey, 'activity'] });
  };

  const statusMutation = useMutation({
    mutationFn: (statusId: string) => issueApi.transition(issueKey, { statusId }),
    onSuccess: () => {
      toast.success('Status updated');
      invalidateIssue();
    },
    onError: (e: unknown) => toast.error('Could not update status', e instanceof Error ? e.message : undefined),
  });

  const assigneeMutation = useMutation({
    mutationFn: (assigneeId: string | null) => issueApi.assign(issueKey, assigneeId),
    onSuccess: () => {
      toast.success('Assignee updated');
      invalidateIssue();
    },
    onError: (e: unknown) => toast.error('Could not update assignee', e instanceof Error ? e.message : undefined),
  });

  if (issueQuery.isLoading) return <FullPageSpinner label="Loading issue…" />;
  if (issueQuery.isError || !issue) return <ErrorState title="Issue not found" onRetry={() => issueQuery.refetch()} />;

  return (
    <div>
      <div className="mb-4 flex items-center gap-1 text-sm text-content-subtle">
        <Link to={`/projects/${issue.projectKey}`} className="hover:text-content">
          {issue.projectName ?? issue.projectKey}
        </Link>
        <ChevronRight size={14} />
        <span className="flex items-center gap-1.5">
          <IssueTypeIcon type={issue.type} size={15} />
          <span className="font-mono">{issue.key}</span>
        </span>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Main column */}
        <div className="space-y-6 lg:col-span-2">
          <h1 className="text-2xl font-semibold tracking-tight text-content">{issue.summary}</h1>

          <section>
            <h2 className="mb-2 text-sm font-semibold text-content-muted">Description</h2>
            {issue.description ? (
              <div className="prose prose-sm max-w-none whitespace-pre-wrap rounded-lg border border-border bg-surface p-4 text-sm text-content">
                {issue.description}
              </div>
            ) : (
              <p className="rounded-lg border border-dashed border-border bg-surface/40 p-4 text-sm italic text-content-subtle">
                No description provided.
              </p>
            )}
          </section>

          {/* Activity tabs */}
          <section>
            <div className="mb-3 flex gap-1 border-b border-border">
              <TabButton active={tab === 'comments'} onClick={() => setTab('comments')} icon={MessageSquare}>
                Comments
              </TabButton>
              <TabButton active={tab === 'history'} onClick={() => setTab('history')} icon={History}>
                History
              </TabButton>
            </div>

            {tab === 'comments' ? (
              <CommentsTab issueKey={issueKey} canComment={canComment} />
            ) : (
              <HistoryTab issueKey={issueKey} />
            )}
          </section>
        </div>

        {/* Sidebar */}
        <aside className="space-y-4">
          <div className="rounded-lg border border-border bg-surface p-4 shadow-card">
            <div className="space-y-4">
              <Field label="Status">
                {canEditIssues && statuses ? (
                  <Select
                    value={issue.status.id}
                    onChange={(e) => statusMutation.mutate(e.target.value)}
                    disabled={statusMutation.isPending}
                    className="h-8 text-sm"
                  >
                    {statuses.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name}
                      </option>
                    ))}
                  </Select>
                ) : (
                  <IssueStatusBadge status={issue.status} />
                )}
              </Field>

              <Field label="Assignee">
                {canEditIssues ? (
                  <Select
                    value={issue.assignee?.id ?? ''}
                    onChange={(e) => assigneeMutation.mutate(e.target.value || null)}
                    disabled={assigneeMutation.isPending}
                    className="h-8 text-sm"
                  >
                    <option value="">Unassigned</option>
                    {(members ?? []).map((m) => (
                      <option key={m.user.id} value={m.user.id}>
                        {m.user.fullName}
                      </option>
                    ))}
                  </Select>
                ) : (
                  <UserLine user={issue.assignee} fallback="Unassigned" />
                )}
              </Field>

              <Field label="Reporter">
                <UserLine user={issue.reporter} fallback="—" />
              </Field>

              <Field label="Priority">
                <IssuePriorityIcon priority={issue.priority} withLabel />
              </Field>

              <Field label="Type">
                <IssueTypeIcon type={issue.type} withLabel />
              </Field>

              {typeof issue.storyPoints === 'number' ? (
                <Field label="Story points">
                  <span className="text-sm text-content">{issue.storyPoints}</span>
                </Field>
              ) : null}

              {issue.sprintName ? (
                <Field label="Sprint">
                  <span className="text-sm text-content">{issue.sprintName}</span>
                </Field>
              ) : null}

              {issue.dueDate ? (
                <Field label="Due date">
                  <span className="text-sm text-content">{formatDateTime(issue.dueDate)}</span>
                </Field>
              ) : null}

              {issue.labels.length > 0 ? (
                <Field label="Labels">
                  <div className="flex flex-wrap gap-1">
                    {issue.labels.map((l) => (
                      <Badge key={l.id} tone="neutral">
                        {l.name}
                      </Badge>
                    ))}
                  </div>
                </Field>
              ) : null}

              {issue.components.length > 0 ? (
                <Field label="Components">
                  <div className="flex flex-wrap gap-1">
                    {issue.components.map((c) => (
                      <Badge key={c.id} tone="accent">
                        {c.name}
                      </Badge>
                    ))}
                  </div>
                </Field>
              ) : null}
            </div>
          </div>

          <p className="px-1 text-xs text-content-subtle">
            Created {formatRelative(issue.createdAt)} · Updated {formatRelative(issue.updatedAt)}
          </p>
        </aside>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[7rem_1fr] items-center gap-2">
      <span className="text-xs font-medium uppercase tracking-wide text-content-subtle">{label}</span>
      <div className="min-w-0">{children}</div>
    </div>
  );
}

function UserLine({ user, fallback }: { user?: { fullName: string; avatarUrl?: string | null } | null; fallback: string }) {
  if (!user) return <span className="text-sm text-content-subtle">{fallback}</span>;
  return (
    <span className="flex items-center gap-2">
      <Avatar user={user as never} size={22} />
      <span className="truncate text-sm text-content">{user.fullName}</span>
    </span>
  );
}

function TabButton({
  active,
  onClick,
  icon: Icon,
  children,
}: {
  active: boolean;
  onClick: () => void;
  icon: typeof MessageSquare;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        '-mb-px flex items-center gap-1.5 border-b-2 px-3 py-2 text-sm font-medium transition-colors',
        active ? 'border-accent text-accent' : 'border-transparent text-content-muted hover:text-content',
      )}
    >
      <Icon size={15} />
      {children}
    </button>
  );
}

function CommentsTab({ issueKey, canComment }: { issueKey: string; canComment: boolean }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['issue', issueKey, 'comments'],
    queryFn: () => commentApi.list(issueKey),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CommentFormValues>({ resolver: zodResolver(commentSchema) });

  const mutation = useMutation({
    mutationFn: (body: string) => commentApi.create(issueKey, body),
    onSuccess: () => {
      reset({ body: '' });
      void queryClient.invalidateQueries({ queryKey: ['issue', issueKey, 'comments'] });
      void queryClient.invalidateQueries({ queryKey: ['issue', issueKey, 'activity'] });
    },
    onError: (e: unknown) => toast.error('Could not post comment', e instanceof Error ? e.message : undefined),
  });

  return (
    <div className="space-y-4">
      {canComment ? (
        <form onSubmit={handleSubmit((v) => mutation.mutate(v.body))} className="space-y-2">
          <Textarea placeholder="Add a comment…" rows={3} error={errors.body?.message} {...register('body')} />
          <div className="flex justify-end">
            <Button type="submit" size="sm" loading={mutation.isPending}>
              <Send size={14} />
              Comment
            </Button>
          </div>
        </form>
      ) : null}

      {isLoading ? (
        <p className="py-4 text-center text-sm text-content-subtle">Loading comments…</p>
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : data && data.length > 0 ? (
        <ul className="space-y-4">
          {data.map((comment) => (
            <li key={comment.id} className="flex gap-3">
              <Avatar user={comment.author} size={30} />
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium text-content">{comment.author.fullName}</span>
                  <span className="text-xs text-content-subtle">{formatRelative(comment.createdAt)}</span>
                  {comment.edited ? <span className="text-xs text-content-subtle">(edited)</span> : null}
                </div>
                <div className="mt-1 whitespace-pre-wrap rounded-lg border border-border bg-surface p-3 text-sm text-content">
                  {comment.body}
                </div>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <p className="py-6 text-center text-sm text-content-subtle">No comments yet. Start the conversation.</p>
      )}
    </div>
  );
}

function HistoryTab({ issueKey }: { issueKey: string }) {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['issue', issueKey, 'activity'],
    queryFn: () => commentApi.activity(issueKey),
  });

  if (isLoading) return <p className="py-4 text-center text-sm text-content-subtle">Loading history…</p>;
  if (isError) return <ErrorState onRetry={() => refetch()} />;
  if (!data || data.length === 0)
    return <p className="py-6 text-center text-sm text-content-subtle">No history recorded yet.</p>;

  return (
    <ul className="space-y-4">
      {data.map((entry) => (
        <li key={entry.id} className="flex gap-3">
          <Avatar user={entry.actor} size={28} />
          <div className="min-w-0 flex-1 text-sm">
            <span className="font-medium text-content">{entry.actor.fullName}</span>{' '}
            <span className="text-content-muted">{describeHistory(entry)}</span>
            <div className="text-xs text-content-subtle">{formatDateTime(entry.createdAt)}</div>
          </div>
        </li>
      ))}
    </ul>
  );
}

function describeHistory(entry: ActivityEntry): string {
  switch (entry.type) {
    case 'CREATED':
      return 'created this issue';
    case 'STATUS_CHANGED':
      return `changed status${entry.from ? ` from ${entry.from}` : ''}${entry.to ? ` to ${entry.to}` : ''}`;
    case 'ASSIGNED':
      return entry.to ? `assigned this to ${entry.to}` : 'cleared the assignee';
    case 'PRIORITY_CHANGED':
      return `changed priority${entry.to ? ` to ${entry.to}` : ''}`;
    case 'SPRINT_CHANGED':
      return entry.to ? `moved this to ${entry.to}` : 'moved this to the backlog';
    case 'COMMENTED':
      return 'added a comment';
    default:
      return `updated ${entry.field ?? 'the issue'}`;
  }
}
