import { api, unwrap } from './client';
import type { ApiResponse, Page, PageParams, Project, ProjectMember } from '@/types';

export interface CreateProjectPayload {
  key: string;
  name: string;
  description?: string;
  leadId?: string;
}

export type UpdateProjectPayload = Partial<Omit<CreateProjectPayload, 'key'>>;

export const projectApi = {
  list: (params?: PageParams & { search?: string }) =>
    api.get<ApiResponse<Page<Project>>>('/projects', { params }).then(unwrap),

  listAll: () => api.get<ApiResponse<Project[]>>('/projects/all').then(unwrap),

  get: (projectKey: string) =>
    api.get<ApiResponse<Project>>(`/projects/${projectKey}`).then(unwrap),

  create: (payload: CreateProjectPayload) =>
    api.post<ApiResponse<Project>>('/projects', payload).then(unwrap),

  update: (projectKey: string, payload: UpdateProjectPayload) =>
    api.patch<ApiResponse<Project>>(`/projects/${projectKey}`, payload).then(unwrap),

  remove: (projectKey: string) =>
    api.delete<ApiResponse<null>>(`/projects/${projectKey}`).then(unwrap),

  members: (projectKey: string) =>
    api.get<ApiResponse<ProjectMember[]>>(`/projects/${projectKey}/members`).then(unwrap),

  addMember: (projectKey: string, payload: { userId: string; role: string }) =>
    api.post<ApiResponse<ProjectMember>>(`/projects/${projectKey}/members`, payload).then(unwrap),

  removeMember: (projectKey: string, userId: string) =>
    api.delete<ApiResponse<null>>(`/projects/${projectKey}/members/${userId}`).then(unwrap),
};
