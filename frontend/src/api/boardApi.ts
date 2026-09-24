import { api, unwrap } from './client';
import type { ApiResponse, Board, Issue } from '@/types';

export interface BoardData {
  board: Board;
  issues: Issue[];
}

export const boardApi = {
  get: (projectKey: string, params?: { sprintId?: string }) =>
    api.get<ApiResponse<BoardData>>(`/projects/${projectKey}/board`, { params }).then(unwrap),
};
