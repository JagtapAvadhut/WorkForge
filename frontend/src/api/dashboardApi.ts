import { api, unwrap } from './client';
import type { ActivityEntry, ApiResponse, DashboardStats, Issue } from '@/types';

export const dashboardApi = {
  stats: () => api.get<ApiResponse<DashboardStats>>('/dashboard/stats').then(unwrap),

  myOpenIssues: () =>
    api.get<ApiResponse<Issue[]>>('/dashboard/my-open-issues').then(unwrap),

  assignedToMe: () =>
    api.get<ApiResponse<Issue[]>>('/dashboard/assigned-to-me').then(unwrap),

  recentActivity: () =>
    api.get<ApiResponse<ActivityEntry[]>>('/dashboard/activity').then(unwrap),
};
