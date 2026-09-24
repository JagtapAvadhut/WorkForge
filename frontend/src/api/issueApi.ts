import { api, unwrap } from './client';
import type {
  ApiResponse,
  Component,
  Issue,
  IssuePriority,
  IssueStatus,
  IssueType,
  Label,
  Page,
  PageParams,
} from '@/types';

export interface IssueFilterParams extends PageParams {
  projectKey?: string;
  search?: string;
  status?: string;
  type?: IssueType;
  priority?: IssuePriority;
  assigneeId?: string;
  reporterId?: string;
  sprintId?: string;
  labels?: string[];
}

export interface CreateIssuePayload {
  projectKey: string;
  type: IssueType;
  summary: string;
  description?: string;
  priority: IssuePriority;
  assigneeId?: string | null;
  sprintId?: string | null;
  labelIds?: string[];
  componentIds?: string[];
  storyPoints?: number | null;
  dueDate?: string | null;
}

export type UpdateIssuePayload = Partial<CreateIssuePayload> & {
  statusId?: string;
};

export const issueApi = {
  list: (params?: IssueFilterParams) =>
    api.get<ApiResponse<Page<Issue>>>('/issues', { params }).then(unwrap),

  myWork: (params?: PageParams) =>
    api.get<ApiResponse<Page<Issue>>>('/issues/my-work', { params }).then(unwrap),

  get: (issueKey: string) => api.get<ApiResponse<Issue>>(`/issues/${issueKey}`).then(unwrap),

  create: (payload: CreateIssuePayload) =>
    api.post<ApiResponse<Issue>>('/issues', payload).then(unwrap),

  update: (issueKey: string, payload: UpdateIssuePayload) =>
    api.patch<ApiResponse<Issue>>(`/issues/${issueKey}`, payload).then(unwrap),

  /** Board drag-and-drop uses this to persist status transitions. */
  transition: (issueKey: string, payload: { statusId: string; rank?: string }) =>
    api.patch<ApiResponse<Issue>>(`/issues/${issueKey}/status`, payload).then(unwrap),

  assign: (issueKey: string, assigneeId: string | null) =>
    api.patch<ApiResponse<Issue>>(`/issues/${issueKey}/assignee`, { assigneeId }).then(unwrap),

  remove: (issueKey: string) =>
    api.delete<ApiResponse<null>>(`/issues/${issueKey}`).then(unwrap),

  statuses: (projectKey: string) =>
    api.get<ApiResponse<IssueStatus[]>>(`/projects/${projectKey}/statuses`).then(unwrap),

  labels: (projectKey: string) =>
    api.get<ApiResponse<Label[]>>(`/projects/${projectKey}/labels`).then(unwrap),

  components: (projectKey: string) =>
    api.get<ApiResponse<Component[]>>(`/projects/${projectKey}/components`).then(unwrap),
};
