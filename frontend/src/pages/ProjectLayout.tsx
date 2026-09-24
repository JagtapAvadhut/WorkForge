import { NavLink, Outlet, useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronRight, Plus } from 'lucide-react';
import { projectApi } from '@/api/projectApi';
import { Button } from '@/components/ui/Button';
import { FullPageSpinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { useUiStore } from '@/stores/uiStore';
import { cn } from '@/utils/cn';

const tabs = [
  { to: '', label: 'Overview', end: true },
  { to: 'board', label: 'Board' },
  { to: 'backlog', label: 'Backlog' },
  { to: 'issues', label: 'Issues' },
  { to: 'settings', label: 'Settings' },
];

export default function ProjectLayout() {
  const { projectKey = '' } = useParams();
  const openCreateIssue = useUiStore((s) => s.openCreateIssue);

  const { data: project, isLoading, isError, refetch } = useQuery({
    queryKey: ['project', projectKey],
    queryFn: () => projectApi.get(projectKey),
    enabled: Boolean(projectKey),
  });

  if (isLoading) return <FullPageSpinner label="Loading project…" />;
  if (isError || !project) return <ErrorState title="Project not found" onRetry={() => refetch()} />;

  return (
    <div>
      <div className="mb-1 flex items-center gap-1 text-sm text-content-subtle">
        <Link to="/projects" className="hover:text-content">
          Projects
        </Link>
        <ChevronRight size={14} />
        <span className="font-mono">{project.key}</span>
      </div>

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-lg bg-accent/10 font-mono text-base font-semibold text-accent">
            {project.key.slice(0, 2)}
          </span>
          <div>
            <h1 className="text-xl font-semibold tracking-tight text-content">{project.name}</h1>
            {project.description ? (
              <p className="line-clamp-1 text-sm text-content-muted">{project.description}</p>
            ) : null}
          </div>
        </div>
        <Button size="sm" onClick={() => openCreateIssue({ projectKey: project.key })}>
          <Plus size={16} />
          Create issue
        </Button>
      </div>

      <div className="mb-6 flex gap-1 border-b border-border">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) =>
              cn(
                '-mb-px border-b-2 px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'border-accent text-accent'
                  : 'border-transparent text-content-muted hover:text-content',
              )
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </div>

      <Outlet context={{ project }} />
    </div>
  );
}
