import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { FolderKanban } from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { projectApi } from '@/api/projectApi';

interface ProjectPickerProps {
  title: string;
  description: string;
  /** Sub-path appended to /projects/:key, e.g. "board" or "backlog". Empty for overview. */
  destination: string;
}

/**
 * Top-level Board / Backlog / Sprints / Reports entries require a project.
 * This page lets the user choose one, then routes into the project-scoped view.
 */
export default function ProjectPicker({ title, description, destination }: ProjectPickerProps) {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['projects', 'all'],
    queryFn: () => projectApi.listAll(),
  });

  return (
    <div>
      <PageHeader title={title} description={description} />

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : !data || data.length === 0 ? (
        <EmptyState
          icon={FolderKanban}
          title="No projects available"
          description="Create a project first to access this view."
          action={
            <Link to="/projects" className="text-sm font-medium text-accent hover:underline">
              Go to projects
            </Link>
          }
        />
      ) : (
        <>
          <p className="mb-3 text-sm text-content-muted">Select a project to continue:</p>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {data.map((project) => (
              <Link
                key={project.id}
                to={`/projects/${project.key}${destination ? `/${destination}` : ''}`}
                className="flex items-center gap-3 rounded-lg border border-border bg-surface p-4 shadow-card transition-all hover:border-accent/50 hover:shadow-raised"
              >
                <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10 font-mono text-sm font-semibold text-accent">
                  {project.key.slice(0, 2)}
                </span>
                <div className="min-w-0">
                  <p className="truncate font-medium text-content">{project.name}</p>
                  <p className="font-mono text-xs text-content-subtle">{project.key}</p>
                </div>
              </Link>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
