import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input, Textarea } from '@/components/ui/Input';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { useToast } from '@/components/ui/Toast';
import { projectApi } from '@/api/projectApi';
import { useProjectContext } from '@/hooks/useProjectContext';
import { usePermissions } from '@/hooks/usePermissions';

const settingsSchema = z.object({
  name: z.string().trim().min(2, 'Name is required').max(80),
  description: z.string().max(2000).optional().or(z.literal('')),
});
type SettingsValues = z.infer<typeof settingsSchema>;

export default function ProjectSettings() {
  const { project } = useProjectContext();
  const queryClient = useQueryClient();
  const toast = useToast();
  const navigate = useNavigate();
  const { canManageProjects } = usePermissions();
  const [confirmDelete, setConfirmDelete] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<SettingsValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: { name: project.name, description: project.description ?? '' },
  });

  const membersQuery = useQuery({
    queryKey: ['project', project.key, 'members'],
    queryFn: () => projectApi.members(project.key),
  });

  const updateMutation = useMutation({
    mutationFn: (values: SettingsValues) =>
      projectApi.update(project.key, { name: values.name, description: values.description || undefined }),
    onSuccess: () => {
      toast.success('Project updated');
      void queryClient.invalidateQueries({ queryKey: ['project', project.key] });
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
    onError: (e: unknown) => toast.error('Could not update project', e instanceof Error ? e.message : undefined),
  });

  const deleteMutation = useMutation({
    mutationFn: () => projectApi.remove(project.key),
    onSuccess: () => {
      toast.success('Project deleted');
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      navigate('/projects');
    },
    onError: (e: unknown) => toast.error('Could not delete project', e instanceof Error ? e.message : undefined),
  });

  if (!canManageProjects) {
    return (
      <EmptyState
        title="Insufficient permissions"
        description="Only project leads and administrators can manage project settings."
      />
    );
  }

  return (
    <div className="max-w-3xl space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>General</CardTitle>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit((v) => updateMutation.mutate(v))} className="space-y-4">
            <Input label="Project name" error={errors.name?.message} {...register('name')} />
            <Input label="Project key" value={project.key} disabled hint="The key cannot be changed after creation." />
            <Textarea label="Description" rows={4} error={errors.description?.message} {...register('description')} />
            <div className="flex justify-end">
              <Button type="submit" loading={updateMutation.isPending} disabled={!isDirty}>
                Save changes
              </Button>
            </div>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Members</CardTitle>
        </CardHeader>
        <CardBody>
          {membersQuery.isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : membersQuery.data && membersQuery.data.length > 0 ? (
            <ul className="divide-y divide-border">
              {membersQuery.data.map((m) => (
                <li key={m.user.id} className="flex items-center gap-3 py-2.5">
                  <Avatar user={m.user} size={30} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-content">{m.user.fullName}</p>
                    <p className="truncate text-xs text-content-subtle">{m.user.email}</p>
                  </div>
                  <Badge tone="neutral">{m.role}</Badge>
                </li>
              ))}
            </ul>
          ) : (
            <p className="py-4 text-center text-sm text-content-subtle">No members yet.</p>
          )}
        </CardBody>
      </Card>

      <Card className="border-danger/30">
        <CardHeader className="border-danger/20">
          <CardTitle className="text-danger">Danger zone</CardTitle>
        </CardHeader>
        <CardBody className="flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-content">Delete this project</p>
            <p className="text-sm text-content-muted">
              Permanently remove the project and all its issues. This cannot be undone.
            </p>
          </div>
          <Button variant="danger" onClick={() => setConfirmDelete(true)}>
            <Trash2 size={16} />
            Delete
          </Button>
        </CardBody>
      </Card>

      <ConfirmDialog
        open={confirmDelete}
        title={`Delete ${project.name}?`}
        description="All issues, sprints, and boards in this project will be permanently deleted."
        confirmLabel="Delete project"
        danger
        loading={deleteMutation.isPending}
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => deleteMutation.mutate()}
      />
    </div>
  );
}
