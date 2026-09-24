import { api, unwrap } from './client';
import type { ApiResponse, SavedFilter } from '@/types';

export interface SaveFilterPayload {
  name: string;
  description?: string;
  jql: string;
  shared?: boolean;
}

export const filterApi = {
  list: () => api.get<ApiResponse<SavedFilter[]>>('/filters').then(unwrap),

  get: (id: string) => api.get<ApiResponse<SavedFilter>>(`/filters/${id}`).then(unwrap),

  create: (payload: SaveFilterPayload) =>
    api.post<ApiResponse<SavedFilter>>('/filters', payload).then(unwrap),

  update: (id: string, payload: Partial<SaveFilterPayload>) =>
    api.patch<ApiResponse<SavedFilter>>(`/filters/${id}`, payload).then(unwrap),

  remove: (id: string) => api.delete<ApiResponse<null>>(`/filters/${id}`).then(unwrap),
};
