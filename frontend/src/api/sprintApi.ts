import { api, unwrap } from './client';
import type { ApiResponse, Issue, Sprint } from '@/types';

export interface CreateSprintPayload {
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
}

export interface BacklogData {
  backlog: Issue[];
  sprints: { sprint: Sprint; issues: Issue[] }[];
}

export const sprintApi = {
  listByProject: (projectKey: string) =>
    api.get<ApiResponse<Sprint[]>>(`/projects/${projectKey}/sprints`).then(unwrap),

  backlog: (projectKey: string) =>
    api.get<ApiResponse<BacklogData>>(`/projects/${projectKey}/backlog`).then(unwrap),

  create: (projectKey: string, payload: CreateSprintPayload) =>
    api.post<ApiResponse<Sprint>>(`/projects/${projectKey}/sprints`, payload).then(unwrap),

  start: (sprintId: string, payload?: { startDate?: string; endDate?: string }) =>
    api.post<ApiResponse<Sprint>>(`/sprints/${sprintId}/start`, payload ?? {}).then(unwrap),

  complete: (sprintId: string, payload?: { moveToSprintId?: string | null }) =>
    api.post<ApiResponse<Sprint>>(`/sprints/${sprintId}/complete`, payload ?? {}).then(unwrap),

  update: (sprintId: string, payload: Partial<CreateSprintPayload>) =>
    api.patch<ApiResponse<Sprint>>(`/sprints/${sprintId}`, payload).then(unwrap),

  remove: (sprintId: string) =>
    api.delete<ApiResponse<null>>(`/sprints/${sprintId}`).then(unwrap),

  /** Move an issue to a sprint (or to the backlog when sprintId is null). */
  moveIssue: (issueKey: string, sprintId: string | null) =>
    api.patch<ApiResponse<Issue>>(`/issues/${issueKey}/sprint`, { sprintId }).then(unwrap),
};
