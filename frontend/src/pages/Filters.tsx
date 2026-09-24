import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Filter, Globe, Lock, Plus, Trash2 } from 'lucide-react';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/Button';
import { Input, Textarea } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { useToast } from '@/components/ui/Toast';
import { filterApi, type SaveFilterPayload } from '@/api/filterApi';
import { formatDate } from '@/utils/format';
import type { SavedFilter } from '@/types';

const filterSchema = z.object({
  name: z.string().trim().min(2, 'Name is required').max(80),
  description: z.string().max(500).optional().or(z.literal('')),
  jql: z.string().trim().min(1, 'Enter a query'),
  shared: z.boolean().optional(),
});
type FilterFormValues = z.infer<typeof filterSchema>;

export default function Filters() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [createOpen, setCreateOpen] = useState(false);
  const [deleting, setDeleting] = useState<SavedFilter | null>(null);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['filters'],
    queryFn: () => filterApi.list(),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => filterApi.remove(id),
    onSuccess: () => {
      toast.success('Filter deleted');
      setDeleting(null);
      void queryClient.invalidateQueries({ queryKey: ['filters'] });
    },
    onError: (e: unknown) => toast.error('Could not delete filter', e instanceof Error ? e.message : undefined),
  });

  return (
    <div>
      <PageHeader
        title="Filters"
        description="Save and reuse issue searches across your projects."
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus size={16} />
            New filter
          </Button>
        }
      />

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : !data || data.length === 0 ? (
        <EmptyState
          icon={Filter}
          title="No saved filters"
          description="Create a filter to quickly access issues that match your criteria."
          action={
            <Button onClick={() => setCreateOpen(true)}>
              <Plus size={16} />
              New filter
            </Button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((f) => (
            <div key={f.id} className="flex flex-col rounded-lg border border-border bg-surface p-4 shadow-card">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/10 text-accent">
                    <Filter size={16} />
                  </span>
                  <h3 className="font-semibold text-content">{f.name}</h3>
                </div>
                <Badge tone={f.shared ? 'info' : 'neutral'}>
                  {f.shared ? <Globe size={11} /> : <Lock size={11} />}
                  {f.shared ? 'Shared' : 'Private'}
                </Badge>
              </div>
              {f.description ? (
                <p className="mt-2 line-clamp-2 text-sm text-content-muted">{f.description}</p>
              ) : null}
              <code className="mt-3 block truncate rounded bg-canvas px-2 py-1 font-mono text-xs text-content-muted">
                {f.jql}
              </code>
              <div className="mt-auto flex items-center justify-between pt-3 text-xs text-content-subtle">
                <span>Created {formatDate(f.createdAt)}</span>
                <button
                  onClick={() => setDeleting(f)}
                  className="flex items-center gap-1 rounded p-1 text-content-subtle hover:text-danger"
                  aria-label="Delete filter"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <CreateFilterModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete filter?"
        description={`"${deleting?.name}" will be permanently removed.`}
        confirmLabel="Delete"
        danger
        loading={deleteMutation.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
      />
    </div>
  );
}

function CreateFilterModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FilterFormValues>({ resolver: zodResolver(filterSchema) });

  const mutation = useMutation({
    mutationFn: (payload: SaveFilterPayload) => filterApi.create(payload),
    onSuccess: () => {
      toast.success('Filter saved');
      reset();
      void queryClient.invalidateQueries({ queryKey: ['filters'] });
      onClose();
    },
    onError: (e: unknown) => toast.error('Could not save filter', e instanceof Error ? e.message : undefined),
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New filter"
      description="Filters use a simple query language, e.g. assignee = me AND status != Done."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="create-filter-form" loading={mutation.isPending}>
            Save filter
          </Button>
        </>
      }
    >
      <form
        id="create-filter-form"
        className="space-y-4"
        onSubmit={handleSubmit((v) =>
          mutation.mutate({ name: v.name, description: v.description || undefined, jql: v.jql, shared: v.shared }),
        )}
      >
        <Input label="Name" placeholder="My open bugs" error={errors.name?.message} {...register('name')} />
        <Input label="Query" placeholder="type = Bug AND status != Done" error={errors.jql?.message} {...register('jql')} />
        <Textarea label="Description" placeholder="Optional" rows={2} error={errors.description?.message} {...register('description')} />
        <label className="flex items-center gap-2 text-sm text-content">
          <input type="checkbox" className="h-4 w-4 rounded border-border-strong text-accent focus:ring-accent" {...register('shared')} />
          Share this filter with my team
        </label>
      </form>
    </Modal>
  );
}
