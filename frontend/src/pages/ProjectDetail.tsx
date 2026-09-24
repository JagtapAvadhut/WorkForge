import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { KanbanSquare, ListTodo, Users } from 'lucide-react';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { IssueCard } from '@/components/issues/IssueCard';
import { issueApi } from '@/api/issueApi';
import { projectApi } from '@/api/projectApi';
import { useProjectContext } from '@/hooks/useProjectContext';

export default function ProjectDetail() {
  const { project } = useProjectContext();

  const issuesQuery = useQuery({
    queryKey: ['issues', { projectKey: project.key, recent: true }],
    queryFn: () => issueApi.list({ projectKey: project.key, size: 8, sort: 'updatedAt,desc' }),
  });
  const membersQuery = useQuery({
    queryKey: ['project', project.key, 'members'],
    queryFn: () => projectApi.members(project.key),
  });

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <div className="space-y-6 lg:col-span-2">
        <div className="grid grid-cols-2 gap-4">
          <Link
            to={`/projects/${project.key}/board`}
            className="flex items-center gap-3 rounded-lg border border-border bg-surface p-4 shadow-card transition-all hover:border-accent/50 hover:shadow-raised"
          >
            <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10 text-accent">
              <KanbanSquare size={20} />
            </span>
            <div>
              <p className="font-medium text-content">Board</p>
              <p className="text-xs text-content-subtle">Visualise work in progress</p>
            </div>
          </Link>
          <Link
            to={`/projects/${project.key}/backlog`}
            className="flex items-center gap-3 rounded-lg border border-border bg-surface p-4 shadow-card transition-all hover:border-accent/50 hover:shadow-raised"
          >
            <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10 text-accent">
              <ListTodo size={20} />
            </span>
            <div>
              <p className="font-medium text-content">Backlog</p>
              <p className="text-xs text-content-subtle">Plan sprints and priorities</p>
            </div>
          </Link>
        </div>

        <Card>
          <CardHeader className="flex items-center justify-between">
            <CardTitle>Recent issues</CardTitle>
            <Link
              to={`/projects/${project.key}/issues`}
              className="text-xs font-medium text-accent hover:underline"
            >
              View all
            </Link>
          </CardHeader>
          <CardBody className="space-y-2">
            {issuesQuery.isLoading ? (
              Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)
            ) : issuesQuery.data && issuesQuery.data.content.length > 0 ? (
              issuesQuery.data.content.map((issue) => <IssueCard key={issue.id} issue={issue} />)
            ) : (
              <EmptyState icon={ListTodo} title="No issues yet" description="Create the first issue for this project." />
            )}
          </CardBody>
        </Card>
      </div>

      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Project details</CardTitle>
          </CardHeader>
          <CardBody className="space-y-3 text-sm">
            <DetailRow label="Key" value={<span className="font-mono">{project.key}</span>} />
            <DetailRow
              label="Lead"
              value={
                project.lead ? (
                  <span className="flex items-center gap-2">
                    <Avatar user={project.lead} size={22} />
                    {project.lead.fullName}
                  </span>
                ) : (
                  <span className="text-content-subtle">Unassigned</span>
                )
              }
            />
            <DetailRow label="Members" value={<Badge tone="neutral">{project.memberCount ?? membersQuery.data?.length ?? 0}</Badge>} />
            <DetailRow label="Open issues" value={<Badge tone="info">{project.openIssueCount ?? 0}</Badge>} />
          </CardBody>
        </Card>

        <Card>
          <CardHeader className="flex items-center gap-2">
            <Users size={16} className="text-content-subtle" />
            <CardTitle>Team</CardTitle>
          </CardHeader>
          <CardBody>
            {membersQuery.isLoading ? (
              <div className="space-y-2">
                {Array.from({ length: 3 }).map((_, i) => (
                  <Skeleton key={i} className="h-8 w-full" />
                ))}
              </div>
            ) : membersQuery.data && membersQuery.data.length > 0 ? (
              <ul className="space-y-2.5">
                {membersQuery.data.map((m) => (
                  <li key={m.user.id} className="flex items-center gap-2.5">
                    <Avatar user={m.user} size={28} />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm text-content">{m.user.fullName}</p>
                    </div>
                    <Badge tone="neutral">{m.role}</Badge>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="py-2 text-center text-sm text-content-subtle">No members yet.</p>
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-content-subtle">{label}</span>
      <span className="font-medium text-content">{value}</span>
    </div>
  );
}
