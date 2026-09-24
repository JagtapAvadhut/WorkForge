import { api, unwrap, type ApiResponse } from './client';
import { getSessionId } from './memoryApi';

export interface McpStatus {
  serverUp: boolean;
  clientConnected: boolean;
  endpoint: string;
  detail: string;
}

export interface McpToolDescriptor {
  name: string;
  description: string;
  parameters: Record<string, unknown>;
}

export interface McpChatResult {
  response: string;
  toolsUsed: Record<string, unknown>[];
}

export interface McpAgentResult {
  response: string;
  completed: boolean;
  iterations: number;
  status: string;
  currentNode: string;
  steps: { node: string; action: string | null }[];
  toolsUsed: Record<string, unknown>[];
  conversationId?: string | null;
}

export const mcpApi = {
  status: async (): Promise<McpStatus> => {
    const { data } = await api.get<ApiResponse<McpStatus>>('/ai/mcp/status');
    return unwrap(data);
  },
  tools: async (): Promise<McpToolDescriptor[]> => {
    const { data } = await api.get<ApiResponse<McpToolDescriptor[]>>('/ai/mcp/tools');
    return unwrap(data);
  },
  chat: async (message: string): Promise<McpChatResult> => {
    const { data } = await api.post<ApiResponse<McpChatResult>>('/ai/mcp-chat', { message });
    return unwrap(data);
  },
  agent: async (
    message: string,
    maxIterations = 5,
    conversationId?: string | null,
  ): Promise<McpAgentResult> => {
    const { data } = await api.post<ApiResponse<McpAgentResult>>('/ai/mcp-agent', {
      message,
      maxIterations,
      conversationId: conversationId || undefined,
      sessionId: getSessionId(),
    });
    return unwrap(data);
  },
};
