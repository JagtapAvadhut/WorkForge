import { api, unwrap } from './client';
import type { ActivityEntry, ApiResponse, Comment } from '@/types';

export const commentApi = {
  list: (issueKey: string) =>
    api.get<ApiResponse<Comment[]>>(`/issues/${issueKey}/comments`).then(unwrap),

  create: (issueKey: string, body: string) =>
    api.post<ApiResponse<Comment>>(`/issues/${issueKey}/comments`, { body }).then(unwrap),

  update: (issueKey: string, commentId: string, body: string) =>
    api
      .patch<ApiResponse<Comment>>(`/issues/${issueKey}/comments/${commentId}`, { body })
      .then(unwrap),

  remove: (issueKey: string, commentId: string) =>
    api.delete<ApiResponse<null>>(`/issues/${issueKey}/comments/${commentId}`).then(unwrap),

  activity: (issueKey: string) =>
    api.get<ApiResponse<ActivityEntry[]>>(`/issues/${issueKey}/activity`).then(unwrap),
};
