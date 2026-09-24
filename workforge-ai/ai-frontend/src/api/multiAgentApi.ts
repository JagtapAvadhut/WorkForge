import { api, unwrap, type ApiResponse } from './client';
import { getSessionId } from './memoryApi';

export interface MultiAgentStep {
  node: string;
  action: string | null;
  details?: Record<string, unknown>;
}

export interface MultiAgentResult {
  answer: string;
  agentsUsed: string[];
  steps: MultiAgentStep[];
  iterations: number;
  specialistCalls: number;
  status: string;
  completed: boolean;
  specialistResults: Record<string, unknown>[];
  sources: Record<string, unknown>[];
  toolCalls: Record<string, unknown>[];
  conversationId?: string | null;
}

export const multiAgentApi = {
  run: async (message: string, conversationId?: string | null): Promise<MultiAgentResult> => {
    const { data } = await api.post<ApiResponse<MultiAgentResult>>('/ai/multi-agent', {
      message,
      conversationId: conversationId || undefined,
      sessionId: getSessionId(),
    });
    return unwrap(data);
  },
};
