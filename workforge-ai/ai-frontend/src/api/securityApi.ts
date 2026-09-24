import { api, unwrap, type ApiResponse } from './client';

export interface SecurityCheckResult {
  status: 'SAFE' | 'SUSPICIOUS' | 'BLOCKED' | string;
  reason: string;
  allowed: boolean;
}

export const securityApi = {
  check: async (input: string): Promise<SecurityCheckResult> => {
    const { data } = await api.post<ApiResponse<SecurityCheckResult>>('/ai/security/check', { input });
    return unwrap(data);
  },
  policies: async (): Promise<Record<string, unknown>> => {
    const { data } = await api.get<ApiResponse<Record<string, unknown>>>('/ai/security/policies');
    return unwrap(data);
  },
};
