import { useQuery } from '@tanstack/react-query';
import { ListChecks } from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { IssueCard } from '@/components/issues/IssueCard';
import { issueApi } from '@/api/issueApi';
import { STATUS_CATEGORY_META } from '@/utils/issueMeta';
import type { Issue, IssueStatusCategory } from '@/types';

const ORDER: IssueStatusCategory[] = ['IN_PROGRESS', 'TODO', 'DONE'];

export default function MyWork() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['my-work'],
    queryFn: () => issueApi.myWork({ size: 100, sort: 'updatedAt,desc' }),
  });

  const grouped = groupByCategory(data?.content ?? []);

  return (
    <div>
      <PageHeader title="My Work" description="Every issue assigned to you, grouped by status." />

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          icon={ListChecks}
          title="Nothing on your plate"
          description="Issues assigned to you will show up here."
        />
      ) : (
        <div className="space-y-6">
          {ORDER.map((category) => {
            const items = grouped[category];
            if (!items || items.length === 0) return null;
            return (
              <section key={category}>
                <h2 className="mb-2 flex items-center gap-2 text-sm font-semibold text-content-muted">
                  <span className={`h-2.5 w-2.5 rounded-full ${STATUS_CATEGORY_META[category].dot}`} />
                  {STATUS_CATEGORY_META[category].label}
                  <span className="text-content-subtle">({items.length})</span>
                </h2>
                <div className="space-y-2">
                  {items.map((issue) => (
                    <IssueCard key={issue.id} issue={issue} />
                  ))}
                </div>
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}

function groupByCategory(issues: Issue[]): Record<IssueStatusCategory, Issue[]> {
  const result: Record<IssueStatusCategory, Issue[]> = { TODO: [], IN_PROGRESS: [], DONE: [] };
  for (const issue of issues) result[issue.status.category].push(issue);
  return result;
}
