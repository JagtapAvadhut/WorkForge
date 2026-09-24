import { api, unwrap } from './client';
import type { ApiResponse, Page, PageParams, Role, User } from '@/types';

export interface UpdateUserPayload {
  fullName?: string;
  active?: boolean;
  roles?: Role[];
}

export const userApi = {
  list: (params?: PageParams & { search?: string }) =>
    api.get<ApiResponse<Page<User>>>('/users', { params }).then(unwrap),

  /** Lightweight lookup for assignee pickers. */
  search: (query: string, projectKey?: string) =>
    api
      .get<ApiResponse<User[]>>('/users/search', { params: { q: query, projectKey } })
      .then(unwrap),

  get: (userId: string) => api.get<ApiResponse<User>>(`/users/${userId}`).then(unwrap),

  update: (userId: string, payload: UpdateUserPayload) =>
    api.patch<ApiResponse<User>>(`/users/${userId}`, payload).then(unwrap),

  updateRoles: (userId: string, roles: Role[]) =>
    api.put<ApiResponse<User>>(`/users/${userId}/roles`, { roles }).then(unwrap),

  deactivate: (userId: string) =>
    api.post<ApiResponse<User>>(`/users/${userId}/deactivate`, {}).then(unwrap),

  activate: (userId: string) =>
    api.post<ApiResponse<User>>(`/users/${userId}/activate`, {}).then(unwrap),
};
