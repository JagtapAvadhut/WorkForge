import { api, unwrap, type ApiResponse } from './client';
import { getSessionId } from './memoryApi';

export interface DemoSeedResult {
  documentsCreated: number;
  documentsSkipped: number;
  documentTopics: string[];
  memoryCreated: boolean;
  memoryId: string | null;
}

export const demoApi = {
  seed: async (): Promise<DemoSeedResult> => {
    const { data } = await api.post<ApiResponse<DemoSeedResult>>(
      '/ai/demo/seed',
      null,
      { params: { sessionId: getSessionId() }, timeout: 180_000 },
    );
    return unwrap(data);
  },
};
