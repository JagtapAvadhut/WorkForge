import { api, unwrap, type ApiResponse } from './client';

export interface EvaluationCaseResult {
  caseId: string;
  category: string;
  passed: boolean;
  expected: string | null;
  actualAnswer: string | null;
  expectedTools: string[];
  actualTools: string[];
  expectedAgents: string[];
  actualAgents: string[];
  latencyMs: number;
  error: string | null;
  traceId: string | null;
}

export interface EvaluationRunSnapshot {
  runId: string;
  suite: string;
  status: string;
  totalCases: number;
  completedCount: number;
  passedCount: number;
  failedCount: number;
  passRate: number;
  averageLatencyMs: number;
  elapsedMs: number;
  currentCaseId: string | null;
  currentCategory: string | null;
  error: string | null;
  startedAt: string;
  completedAt: string | null;
  results: EvaluationCaseResult[];
}

export type EvaluationSuite = 'SMOKE' | 'CORE' | 'ALL';

export const evaluationApi = {
  start: async (suite: EvaluationSuite): Promise<EvaluationRunSnapshot> => {
    const { data } = await api.post<ApiResponse<EvaluationRunSnapshot>>(
      '/ai/evaluation/run',
      { suite },
      { timeout: 15_000 },
    );
    return unwrap(data);
  },
  getRun: async (runId: string): Promise<EvaluationRunSnapshot> => {
    const { data } = await api.get<ApiResponse<EvaluationRunSnapshot>>(
      `/ai/evaluation/runs/${runId}`,
      { timeout: 15_000 },
    );
    return unwrap(data);
  },
};
