import { api, unwrap, type ApiResponse } from './client';
import { getSessionId } from './memoryApi';

export interface AgentStep {
  stepNumber: number;
  thought: string | null;
  action: string;
  tool: string | null;
  arguments: Record<string, unknown>;
  observation: string | null;
  status: string;
}

export interface AgentResult {
  answer: string;
  steps: AgentStep[];
  stopReason: string;
  iterations: number;
  conversationId?: string | null;
}

export const agentApi = {
  run: async (message: string, maxSteps = 5, conversationId?: string | null): Promise<AgentResult> => {
    const { data } = await api.post<ApiResponse<AgentResult>>('/ai/agent/run', {
      message,
      maxSteps,
      conversationId: conversationId || undefined,
      sessionId: getSessionId(),
    });
    return unwrap(data);
  },
};
