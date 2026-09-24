import { api, unwrap, type ApiResponse } from './client';

export interface RagSource {
  id: string;
  content: string;
  metadata: Record<string, unknown>;
  similarity: number;
}

export interface RagQueryResult {
  answer: string;
  sources: RagSource[];
}

export const ragApi = {
  query: async (question: string, topK: number): Promise<RagQueryResult> => {
    const { data } = await api.post<ApiResponse<RagQueryResult>>('/ai/rag/query', {
      question,
      topK,
    });
    return unwrap(data);
  },
};
