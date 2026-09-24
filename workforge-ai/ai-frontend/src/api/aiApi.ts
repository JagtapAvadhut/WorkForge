import { api, unwrap, type ApiResponse } from './client';
import { getSessionId } from './memoryApi';
import type { ChatRole, PromptStrategy } from '@/types/chat';

export interface ChatResult {
  response: string;
  strategy: PromptStrategy;
  conversationId?: string | null;
}

export interface HistoryPayload {
  role: ChatRole;
  content: string;
}

export interface ChatPayload {
  message: string;
  strategy: PromptStrategy;
  history?: HistoryPayload[];
  conversationId?: string | null;
}

export const aiApi = {
  chat: async (payload: ChatPayload): Promise<ChatResult> => {
    const { data } = await api.post<ApiResponse<ChatResult>>('/ai/chat', {
      ...payload,
      sessionId: getSessionId(),
    });
    return unwrap(data);
  },
};
