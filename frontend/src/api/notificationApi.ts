import { api, unwrap } from './client';
import type { ApiResponse, AppNotification, Page, PageParams } from '@/types';

export const notificationApi = {
  list: (params?: PageParams & { unreadOnly?: boolean }) =>
    api.get<ApiResponse<Page<AppNotification>>>('/notifications', { params }).then(unwrap),

  unreadCount: () =>
    api.get<ApiResponse<{ count: number }>>('/notifications/unread-count').then(unwrap),

  markRead: (id: string) =>
    api.post<ApiResponse<AppNotification>>(`/notifications/${id}/read`, {}).then(unwrap),

  markAllRead: () =>
    api.post<ApiResponse<{ updated: number }>>('/notifications/read-all', {}).then(unwrap),
};
