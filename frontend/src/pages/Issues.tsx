import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { ListTodo, Plus, Search, X } from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/Button';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { IssueCard } from '@/components/issues/IssueCard';
import { issueApi, type IssueFilterParams } from '@/api/issueApi';
import { useProjectStatuses } from '@/hooks/useReferenceData';
import { useDebounce } from '@/hooks/useDebounce';
import { useUiStore } from '@/stores/uiStore';
import { ISSUE_PRIORITIES, ISSUE_TYPES, ISSUE_TYPE_META, PRIORITY_META } from '@/utils/issueMeta';
import type { IssuePriority, IssueType } from '@/types';

export default function Issues() {
  const { projectKey } = useParams();
  const openCreateIssue = useUiStore((s) => s.openCreateIssue);

  const [search, setSearch] = useState('');
  const [type, setType] = useState<IssueType | ''>('');
  const [priority, setPriority] = useState<IssuePriority | ''>('');
  const [statusId, setStatusId] = useState('');
  const [page, setPage] = useState(0);

  const debounced = useDebounce(search.trim(), 300);
  const { data: statuses } = useProjectStatuses(projectKey);

  const params: IssueFilterParams = {
    projectKey,
    search: debounced || undefined,
    type: type || undefined,
    priority: priority || undefined,
    status: statusId || undefined,
    page,
    size: 20,
    sort: 'updatedAt,desc',
  };

  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['issues', params],
    queryFn: () => issueApi.list(params),
    placeholderData: keepPreviousData,
  });

  const hasFilters = Boolean(debounced || type || priority || statusId);
  const clearFilters = () => {
    setSearch('');
    setType('');
    setPriority('');
    setStatusId('');
    setPage(0);
  };

  const issues = data?.content ?? [];

  return (
    <div>
      {!projectKey ? (
        <PageHeader
          title="Issues"
          description="Search and filter issues across all your projects."
          actions={
            <Button onClick={() => openCreateIssue()}>
              <Plus size={16} />
              Create issue
            </Button>
          }
        />
      ) : null}

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="relative min-w-[14rem] flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-content-subtle" />
          <input
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            placeholder="Search issues…"
            className="h-9 w-full rounded-md border border-border bg-surface pl-9 pr-3 text-sm text-content placeholder:text-content-subtle focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
          />
        </div>
        <Select
          value={type}
          onChange={(e) => {
            setType(e.target.value as IssueType | '');
            setPage(0);
          }}
          className="h-9 w-auto min-w-[8rem]"
        >
          <option value="">All types</option>
          {ISSUE_TYPES.map((t) => (
            <option key={t} value={t}>
              {ISSUE_TYPE_META[t].label}
            </option>
          ))}
        </Select>
        <Select
          value={priority}
          onChange={(e) => {
            setPriority(e.target.value as IssuePriority | '');
            setPage(0);
          }}
          className="h-9 w-auto min-w-[8rem]"
        >
          <option value="">All priorities</option>
          {ISSUE_PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {PRIORITY_META[p].label}
            </option>
          ))}
        </Select>
        {projectKey && statuses ? (
          <Select
            value={statusId}
            onChange={(e) => {
              setStatusId(e.target.value);
              setPage(0);
            }}
            className="h-9 w-auto min-w-[8rem]"
          >
            <option value="">All statuses</option>
            {statuses.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </Select>
        ) : null}
        {hasFilters ? (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            <X size={14} />
            Clear
          </Button>
        ) : null}
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : issues.length === 0 ? (
        <EmptyState
          icon={ListTodo}
          title="No issues found"
          description={hasFilters ? 'Try adjusting your filters.' : 'Create an issue to get started.'}
          action={
            <Button onClick={() => openCreateIssue(projectKey ? { projectKey } : undefined)}>
              <Plus size={16} />
              Create issue
            </Button>
          }
        />
      ) : (
        <>
          <div className="space-y-2">
            {issues.map((issue) => (
              <IssueCard key={issue.id} issue={issue} />
            ))}
          </div>

          {data && data.totalPages > 1 ? (
            <div className="mt-4 flex items-center justify-between">
              <p className="text-sm text-content-subtle">
                Page {data.page + 1} of {data.totalPages} · {data.totalElements} issues
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={data.first || isFetching}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={data.last || isFetching}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          ) : null}
        </>
      )}
    </div>
  );
}
