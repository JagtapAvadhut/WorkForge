import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Activity,
  CircleDot,
  Clock,
  ListChecks,
  TrendingUp,
  UserCheck,
} from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { Avatar } from '@/components/ui/Avatar';
import { IssueCard } from '@/components/issues/IssueCard';
import { dashboardApi } from '@/api/dashboardApi';
import { useAuthStore } from '@/stores/authStore';
import { STATUS_CATEGORY_META } from '@/utils/issueMeta';
import { formatRelative } from '@/utils/format';
import { cn } from '@/utils/cn';
import type { LucideIcon } from 'lucide-react';
import type { UseQueryResult } from '@tanstack/react-query';
import type { Issue } from '@/types';

export default function Dashboard() {
  const user = useAuthStore((s) => s.user);

  const statsQuery = useQuery({ queryKey: ['dashboard', 'stats'], queryFn: () => dashboardApi.stats() });
  const openQuery = useQuery({
    queryKey: ['dashboard', 'my-open'],
    queryFn: () => dashboardApi.myOpenIssues(),
  });
  const assignedQuery = useQuery({
    queryKey: ['dashboard', 'assigned'],
    queryFn: () => dashboardApi.assignedToMe(),
  });
  const activityQuery = useQuery({
    queryKey: ['dashboard', 'activity'],
    queryFn: () => dashboardApi.recentActivity(),
  });

  const stats = statsQuery.data;

  return (
    <div>
      <PageHeader
        title={`Good to see you, ${user?.fullName?.split(' ')[0] ?? 'there'}`}
        description="Here's what's happening across your work today."
      />

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard
          icon={CircleDot}
          label="Open issues"
          value={stats?.openIssues}
          loading={statsQuery.isLoading}
          tone="text-info"
        />
        <StatCard
          icon={UserCheck}
          label="Assigned to me"
          value={stats?.assignedToMe}
          loading={statsQuery.isLoading}
          tone="text-accent"
        />
        <StatCard
          icon={ListChecks}
          label="Reported by me"
          value={stats?.reportedByMe}
          loading={statsQuery.isLoading}
          tone="text-success"
        />
        <StatCard
          icon={Clock}
          label="Due soon"
          value={stats?.dueSoon}
          loading={statsQuery.isLoading}
          tone="text-warning"
        />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <CardHeader className="flex items-center justify-between">
              <CardTitle>Assigned to me</CardTitle>
              <Link to="/my-work" className="text-xs font-medium text-accent hover:underline">
                View all
              </Link>
            </CardHeader>
            <CardBody className="space-y-2">
              <IssueListSection
                query={assignedQuery}
                emptyTitle="Nothing assigned"
                emptyDescription="Issues assigned to you will appear here."
              />
            </CardBody>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>My open issues</CardTitle>
            </CardHeader>
            <CardBody className="space-y-2">
              <IssueListSection
                query={openQuery}
                emptyTitle="All clear"
                emptyDescription="You have no open issues right now."
              />
            </CardBody>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader className="flex items-center gap-2">
              <TrendingUp size={16} className="text-content-subtle" />
              <CardTitle>Status distribution</CardTitle>
            </CardHeader>
            <CardBody>
              {statsQuery.isLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 3 }).map((_, i) => (
                    <Skeleton key={i} className="h-6 w-full" />
                  ))}
                </div>
              ) : stats && stats.statusDistribution.length > 0 ? (
                <StatusDistribution data={stats.statusDistribution} />
              ) : (
                <p className="py-4 text-center text-sm text-content-subtle">No data yet.</p>
              )}
            </CardBody>
          </Card>

          <Card>
            <CardHeader className="flex items-center gap-2">
              <Activity size={16} className="text-content-subtle" />
              <CardTitle>Recent activity</CardTitle>
            </CardHeader>
            <CardBody>
              {activityQuery.isLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 4 }).map((_, i) => (
                    <Skeleton key={i} className="h-10 w-full" />
                  ))}
                </div>
              ) : activityQuery.isError ? (
                <ErrorState onRetry={() => activityQuery.refetch()} />
              ) : activityQuery.data && activityQuery.data.length > 0 ? (
                <ul className="space-y-3">
                  {activityQuery.data.slice(0, 8).map((entry) => (
                    <li key={entry.id} className="flex items-start gap-2.5">
                      <Avatar user={entry.actor} size={26} />
                      <div className="min-w-0 flex-1 text-sm">
                        <span className="font-medium text-content">{entry.actor.fullName}</span>{' '}
                        <span className="text-content-muted">{describeActivity(entry.type)}</span>
                        <div className="text-xs text-content-subtle">
                          {formatRelative(entry.createdAt)}
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="py-4 text-center text-sm text-content-subtle">No recent activity.</p>
              )}
            </CardBody>
          </Card>
        </div>
      </div>
    </div>
  );
}

function describeActivity(type: string): string {
  switch (type) {
    case 'CREATED':
      return 'created an issue';
    case 'STATUS_CHANGED':
      return 'changed a status';
    case 'ASSIGNED':
      return 'updated an assignee';
    case 'COMMENTED':
      return 'left a comment';
    case 'PRIORITY_CHANGED':
      return 'changed a priority';
    case 'SPRINT_CHANGED':
      return 'moved an issue between sprints';
    default:
      return 'updated an issue';
  }
}

function StatCard({
  icon: Icon,
  label,
  value,
  loading,
  tone,
}: {
  icon: LucideIcon;
  label: string;
  value?: number;
  loading?: boolean;
  tone: string;
}) {
  return (
    <Card>
      <CardBody className="flex items-center gap-3">
        <span className={cn('flex h-10 w-10 items-center justify-center rounded-lg bg-canvas', tone)}>
          <Icon size={20} />
        </span>
        <div>
          {loading ? (
            <Skeleton className="h-7 w-10" />
          ) : (
            <p className="text-2xl font-semibold tabular-nums text-content">{value ?? 0}</p>
          )}
          <p className="text-xs text-content-muted">{label}</p>
        </div>
      </CardBody>
    </Card>
  );
}

function StatusDistribution({
  data,
}: {
  data: { category: keyof typeof STATUS_CATEGORY_META; label: string; count: number }[];
}) {
  const total = data.reduce((sum, d) => sum + d.count, 0) || 1;
  return (
    <div className="space-y-3">
      <div className="flex h-2.5 w-full overflow-hidden rounded-full bg-canvas">
        {data.map((d) => (
          <div
            key={d.category}
            className={STATUS_CATEGORY_META[d.category]?.dot ?? 'bg-slate-400'}
            style={{ width: `${(d.count / total) * 100}%` }}
            title={`${d.label}: ${d.count}`}
          />
        ))}
      </div>
      <ul className="space-y-2">
        {data.map((d) => (
          <li key={d.category} className="flex items-center justify-between text-sm">
            <span className="flex items-center gap-2">
              <span
                className={cn('h-2.5 w-2.5 rounded-full', STATUS_CATEGORY_META[d.category]?.dot)}
              />
              <span className="text-content-muted">{d.label}</span>
            </span>
            <span className="font-medium tabular-nums text-content">{d.count}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function IssueListSection({
  query,
  emptyTitle,
  emptyDescription,
}: {
  query: UseQueryResult<Issue[]>;
  emptyTitle: string;
  emptyDescription: string;
}) {
  if (query.isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full" />
        ))}
      </div>
    );
  }
  if (query.isError) {
    return <ErrorState onRetry={() => query.refetch()} />;
  }
  if (!query.data || query.data.length === 0) {
    return <EmptyState icon={ListChecks} title={emptyTitle} description={emptyDescription} />;
  }
  return (
    <>
      {query.data.slice(0, 6).map((issue) => (
        <IssueCard key={issue.id} issue={issue} />
      ))}
    </>
  );
}
