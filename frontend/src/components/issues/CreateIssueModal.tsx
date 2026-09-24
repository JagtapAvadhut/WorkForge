import { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Input, Textarea } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { useToast } from '@/components/ui/Toast';
import { MultiSelect } from '@/components/common/MultiSelect';
import { issueApi, type CreateIssuePayload } from '@/api/issueApi';
import { projectApi } from '@/api/projectApi';
import {
  useProjectLabels,
  useProjectComponents,
  useProjectSprints,
  useProjectsAll,
} from '@/hooks/useReferenceData';
import { createIssueSchema, type CreateIssueFormValues } from '@/utils/schemas';
import { ISSUE_PRIORITIES, ISSUE_TYPES, ISSUE_TYPE_META, PRIORITY_META } from '@/utils/issueMeta';

interface CreateIssueModalProps {
  open: boolean;
  onClose: () => void;
  defaultProjectKey?: string;
  defaultSprintId?: string;
  onCreated?: (issueKey: string) => void;
}

const emptyDefaults: CreateIssueFormValues = {
  projectKey: '',
  type: 'TASK',
  summary: '',
  description: '',
  priority: 'MEDIUM',
  assigneeId: '',
  sprintId: '',
  labels: [],
  components: [],
  storyPoints: '',
  dueDate: '',
};

export function CreateIssueModal({
  open,
  onClose,
  defaultProjectKey,
  defaultSprintId,
  onCreated,
}: CreateIssueModalProps) {
  const toast = useToast();
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    control,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateIssueFormValues>({
    resolver: zodResolver(createIssueSchema),
    defaultValues: { ...emptyDefaults, projectKey: defaultProjectKey ?? '', sprintId: defaultSprintId ?? '' },
  });

  const projectKey = watch('projectKey');

  const { data: projects } = useProjectsAll();
  const { data: labels } = useProjectLabels(projectKey);
  const { data: components } = useProjectComponents(projectKey);
  const { data: sprints } = useProjectSprints(projectKey);
  const { data: members } = useQuery({
    queryKey: ['project', projectKey, 'members'],
    queryFn: () => projectApi.members(projectKey),
    enabled: Boolean(projectKey),
  });

  // Reset when opened, applying provided defaults.
  useEffect(() => {
    if (open) {
      reset({
        ...emptyDefaults,
        projectKey: defaultProjectKey ?? '',
        sprintId: defaultSprintId ?? '',
      });
    }
  }, [open, defaultProjectKey, defaultSprintId, reset]);

  const createMutation = useMutation({
    mutationFn: (payload: CreateIssuePayload) => issueApi.create(payload),
    onSuccess: (issue) => {
      toast.success('Issue created', `${issue.key} — ${issue.summary}`);
      void queryClient.invalidateQueries({ queryKey: ['issues'] });
      void queryClient.invalidateQueries({ queryKey: ['board'] });
      void queryClient.invalidateQueries({ queryKey: ['backlog'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      onCreated?.(issue.key);
      onClose();
    },
    onError: (err: unknown) => {
      toast.error('Could not create issue', err instanceof Error ? err.message : undefined);
    },
  });

  const onSubmit = (values: CreateIssueFormValues) => {
    const payload: CreateIssuePayload = {
      projectKey: values.projectKey,
      type: values.type as CreateIssuePayload['type'],
      summary: values.summary,
      description: values.description || undefined,
      priority: values.priority as CreateIssuePayload['priority'],
      assigneeId: values.assigneeId || null,
      sprintId: values.sprintId || null,
      labelIds: values.labels ?? [],
      componentIds: values.components ?? [],
      storyPoints: values.storyPoints === '' || values.storyPoints == null ? null : Number(values.storyPoints),
      dueDate: values.dueDate || null,
    };
    createMutation.mutate(payload);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create issue"
      description="Capture work to be tracked in a project."
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="create-issue-form" loading={isSubmitting || createMutation.isPending}>
            Create issue
          </Button>
        </>
      }
    >
      <form id="create-issue-form" onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Select label="Project" error={errors.projectKey?.message} {...register('projectKey')} placeholder="Select a project">
            {(projects ?? []).map((p) => (
              <option key={p.id} value={p.key}>
                {p.name} ({p.key})
              </option>
            ))}
          </Select>
          <Select label="Issue type" error={errors.type?.message} {...register('type')}>
            {ISSUE_TYPES.map((t) => (
              <option key={t} value={t}>
                {ISSUE_TYPE_META[t].label}
              </option>
            ))}
          </Select>
        </div>

        <Input
          label="Summary"
          placeholder="Short, descriptive title"
          error={errors.summary?.message}
          {...register('summary')}
        />

        <Textarea
          label="Description"
          placeholder="Add context, acceptance criteria, links…"
          rows={4}
          error={errors.description?.message}
          {...register('description')}
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Select label="Priority" error={errors.priority?.message} {...register('priority')}>
            {ISSUE_PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {PRIORITY_META[p].label}
              </option>
            ))}
          </Select>
          <Select label="Assignee" {...register('assigneeId')} disabled={!projectKey}>
            <option value="">Unassigned</option>
            {(members ?? []).map((m) => (
              <option key={m.user.id} value={m.user.id}>
                {m.user.fullName}
              </option>
            ))}
          </Select>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Select label="Sprint" {...register('sprintId')} disabled={!projectKey}>
            <option value="">Backlog</option>
            {(sprints ?? []).map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
                {s.state === 'ACTIVE' ? ' (active)' : ''}
              </option>
            ))}
          </Select>
          <Input
            label="Story points"
            type="number"
            min={0}
            step={1}
            placeholder="—"
            error={errors.storyPoints?.message as string | undefined}
            {...register('storyPoints')}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Controller
            control={control}
            name="labels"
            render={({ field }) => (
              <MultiSelect
                label="Labels"
                placeholder={projectKey ? 'Add labels' : 'Select a project first'}
                disabled={!projectKey}
                options={(labels ?? []).map((l) => ({ value: l.id, label: l.name }))}
                value={field.value ?? []}
                onChange={field.onChange}
              />
            )}
          />
          <Controller
            control={control}
            name="components"
            render={({ field }) => (
              <MultiSelect
                label="Components"
                placeholder={projectKey ? 'Add components' : 'Select a project first'}
                disabled={!projectKey}
                options={(components ?? []).map((c) => ({ value: c.id, label: c.name }))}
                value={field.value ?? []}
                onChange={field.onChange}
              />
            )}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input label="Due date" type="date" {...register('dueDate')} />
          <div className="hidden sm:block" />
        </div>
      </form>
    </Modal>
  );
}
