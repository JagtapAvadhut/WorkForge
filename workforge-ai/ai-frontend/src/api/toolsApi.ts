import { api, unwrap, type ApiResponse } from './client';

export interface ToolCallInfo {
  tool: string;
  arguments: Record<string, unknown>;
  resultSummary: string;
}

export interface ToolChatResult {
  response: string;
  toolCalls: ToolCallInfo[];
}

export interface ToolDefinitionView {
  name: string;
  description: string;
  inputSchema: string;
}

export interface ToolPreviewResult {
  message: string;
  tools: ToolDefinitionView[];
}

export const toolsApi = {
  chat: async (message: string): Promise<ToolChatResult> => {
    const { data } = await api.post<ApiResponse<ToolChatResult>>('/ai/tool-chat', { message });
    return unwrap(data);
  },

  preview: async (message: string): Promise<ToolPreviewResult> => {
    const { data } = await api.post<ApiResponse<ToolPreviewResult>>('/ai/tool-preview', { message });
    return unwrap(data);
  },
};
