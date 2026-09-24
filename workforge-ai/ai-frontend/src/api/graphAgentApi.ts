import { api, unwrap, type ApiResponse } from './client';
import { getSessionId } from './memoryApi';

export interface GraphAgentStep {
  node: string;
  action: string | null;
  details?: Record<string, unknown>;
}

export interface GraphAgentResult {
  response: string;
  completed: boolean;
  iterations: number;
  status: string;
  currentNode: string;
  steps: GraphAgentStep[];
  toolCalls: Record<string, unknown>[];
  toolResults: Record<string, unknown>[];
  conversationId?: string | null;
}

export const graphAgentApi = {
  run: async (
    message: string,
    maxIterations = 5,
    conversationId?: string | null,
  ): Promise<GraphAgentResult> => {
    const { data } = await api.post<ApiResponse<GraphAgentResult>>('/ai/graph-agent', {
      message,
      maxIterations,
      conversationId: conversationId || undefined,
      sessionId: getSessionId(),
    });
    return unwrap(data);
  },
};
