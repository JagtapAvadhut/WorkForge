import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { FolderKanban, Plus, Search } from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/Button';
import { Input, Textarea } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Avatar } from '@/components/ui/Avatar';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { useToast } from '@/components/ui/Toast';
import { projectApi, type CreateProjectPayload } from '@/api/projectApi';
import { useDebounce } from '@/hooks/useDebounce';
import { usePermissions } from '@/hooks/usePermissions';
import { createProjectSchema, type CreateProjectFormValues } from '@/utils/schemas';
import { pluralize } from '@/utils/format';

export default function Projects() {
  const [search, setSearch] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const debounced = useDebounce(search.trim(), 300);
  const { canManageProjects } = usePermissions();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['projects', { search: debounced }],
    queryFn: () => projectApi.list({ search: debounced || undefined, size: 50 }),
  });

  const projects = data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Projects"
        description="Browse and manage the projects your team is working on."
        actions={
          canManageProjects ? (
            <Button onClick={() => setCreateOpen(true)}>
              <Plus size={16} />
              New project
            </Button>
          ) : null
        }
      />

      <div className="relative mb-4 max-w-sm">
        <Search
          size={16}
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-content-subtle"
        />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search projects…"
          className="h-9 w-full rounded-md border border-border bg-surface pl-9 pr-3 text-sm text-content placeholder:text-content-subtle focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
        />
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : projects.length === 0 ? (
        <EmptyState
          icon={FolderKanban}
          title="No projects found"
          description={debounced ? 'Try a different search term.' : 'Create your first project to get started.'}
          action={
            canManageProjects && !debounced ? (
              <Button onClick={() => setCreateOpen(true)}>
                <Plus size={16} />
                New project
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <Link
              key={project.id}
              to={`/projects/${project.key}`}
              className="group flex flex-col rounded-lg border border-border bg-surface p-4 shadow-card transition-all hover:border-border-strong hover:shadow-raised"
            >
              <div className="flex items-center gap-3">
                <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10 font-mono text-sm font-semibold text-accent">
                  {project.key.slice(0, 2)}
                </span>
                <div className="min-w-0">
                  <h3 className="truncate font-semibold text-content group-hover:text-accent">
                    {project.name}
                  </h3>
                  <p className="font-mono text-xs text-content-subtle">{project.key}</p>
                </div>
              </div>
              {project.description ? (
                <p className="mt-3 line-clamp-2 text-sm text-content-muted">{project.description}</p>
              ) : (
                <p className="mt-3 text-sm italic text-content-subtle">No description</p>
              )}
              <div className="mt-4 flex items-center justify-between border-t border-border pt-3">
                <span className="text-xs text-content-subtle">
                  {pluralize(project.openIssueCount ?? project.issueCount ?? 0, 'open issue')}
                </span>
                {project.lead ? <Avatar user={project.lead} size={24} /> : null}
              </div>
            </Link>
          ))}
        </div>
      )}

      <CreateProjectModal open={createOpen} onClose={() => setCreateOpen(false)} />
    </div>
  );
}

function CreateProjectModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateProjectFormValues>({ resolver: zodResolver(createProjectSchema) });

  const mutation = useMutation({
    mutationFn: (payload: CreateProjectPayload) => projectApi.create(payload),
    onSuccess: (project) => {
      toast.success('Project created', `${project.name} (${project.key})`);
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      reset();
      onClose();
    },
    onError: (err: unknown) =>
      toast.error('Could not create project', err instanceof Error ? err.message : undefined),
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create project"
      description="Projects group related issues, boards, and sprints."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="create-project-form" loading={mutation.isPending}>
            Create project
          </Button>
        </>
      }
    >
      <form
        id="create-project-form"
        onSubmit={handleSubmit((v) =>
          mutation.mutate({ key: v.key, name: v.name, description: v.description || undefined }),
        )}
        className="space-y-4"
      >
        <Input label="Project name" placeholder="Marketing Website" error={errors.name?.message} {...register('name')} />
        <Input
          label="Project key"
          placeholder="MKT"
          hint="2–10 uppercase letters used as the prefix for issue keys."
          error={errors.key?.message}
          {...register('key')}
        />
        <Textarea label="Description" placeholder="What is this project about?" error={errors.description?.message} {...register('description')} />
      </form>
    </Modal>
  );
}
