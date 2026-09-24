import { api, unwrap, type ApiResponse } from './client';

export interface Conversation {
  id: string;
  sessionId: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MemoryMessage {
  id: string;
  conversationId: string;
  role: string;
  content: string;
  createdAt: string;
}

export interface LongTermMemory {
  id: string;
  sessionId: string;
  category: string;
  content: string;
  source: string;
  importance: number;
  createdAt: string;
  updatedAt: string;
}

const SESSION_KEY = 'workforge-ai-session-id';

export function getSessionId(): string {
  let id = localStorage.getItem(SESSION_KEY);
  if (!id) {
    id = `session-${crypto.randomUUID()}`;
    localStorage.setItem(SESSION_KEY, id);
  }
  return id;
}

export const memoryApi = {
  listConversations: async (): Promise<Conversation[]> => {
    const { data } = await api.get<ApiResponse<Conversation[]>>('/ai/memory/conversations', {
      params: { sessionId: getSessionId() },
    });
    return unwrap(data);
  },
  createConversation: async (title?: string): Promise<Conversation> => {
    const { data } = await api.post<ApiResponse<Conversation>>('/ai/memory/conversations', {
      sessionId: getSessionId(),
      title: title ?? 'New conversation',
    });
    return unwrap(data);
  },
  getMessages: async (id: string): Promise<MemoryMessage[]> => {
    const { data } = await api.get<ApiResponse<MemoryMessage[]>>(`/ai/memory/conversations/${id}/messages`);
    return unwrap(data);
  },
  deleteConversation: async (id: string): Promise<void> => {
    const { data } = await api.delete<ApiResponse<{ deleted: boolean }>>(`/ai/memory/conversations/${id}`);
    unwrap(data);
  },
  listMemories: async (): Promise<LongTermMemory[]> => {
    const { data } = await api.get<ApiResponse<LongTermMemory[]>>('/ai/memory', {
      params: { sessionId: getSessionId() },
    });
    return unwrap(data);
  },
  remember: async (payload: {
    category: string;
    content: string;
    importance: number;
  }): Promise<LongTermMemory> => {
    const { data } = await api.post<ApiResponse<LongTermMemory>>('/ai/memory/remember', {
      ...payload,
      sessionId: getSessionId(),
    });
    return unwrap(data);
  },
  deleteMemory: async (id: string): Promise<void> => {
    const { data } = await api.delete<ApiResponse<{ deleted: boolean }>>(`/ai/memory/${id}`);
    unwrap(data);
  },
};
