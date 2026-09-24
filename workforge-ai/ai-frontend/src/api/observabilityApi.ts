import { api, unwrap, type ApiResponse } from './client';

export interface TraceDto {
  id: string;
  traceId: string;
  feature: string;
  model: string | null;
  startedAt: string;
  completedAt: string | null;
  latencyMs: number | null;
  success: boolean;
  errorCode: string | null;
  toolCallCount: number;
  agentStepCount: number;
  mcpCallCount: number;
  retrievalCount: number;
  inputTokens: number | null;
  outputTokens: number | null;
  totalTokens: number | null;
  requestSummary: string | null;
}

export interface TracePage {
  items: TraceDto[];
  total: number;
  limit: number;
  offset: number;
}

export interface ObservabilitySummary {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  averageLatencyMs: number;
  toolCallCount: number;
  agentExecutions: number;
  mcpExecutions: number;
  ragRetrievalCount: number;
}

export const observabilityApi = {
  summary: async (): Promise<ObservabilitySummary> => {
    const { data } = await api.get<ApiResponse<ObservabilitySummary>>('/ai/observability/summary');
    return unwrap(data);
  },
  traces: async (limit = 20, offset = 0): Promise<TracePage> => {
    const { data } = await api.get<ApiResponse<TracePage>>('/ai/observability/traces', {
      params: { limit, offset },
    });
    return unwrap(data);
  },
  trace: async (traceId: string): Promise<TraceDto> => {
    const { data } = await api.get<ApiResponse<TraceDto>>(`/ai/observability/traces/${traceId}`);
    return unwrap(data);
  },
};
