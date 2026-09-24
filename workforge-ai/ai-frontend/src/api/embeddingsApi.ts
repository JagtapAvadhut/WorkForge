import { api, unwrap, type ApiResponse } from './client';

export interface EmbeddingResult {
  text: string;
  dimensions: number;
  embedding: number[];
}

export interface SimilarityResult {
  similarity: number;
}

export interface BatchEmbeddingResult {
  items: EmbeddingResult[];
}

export interface RankedDocument {
  text: string;
  similarity: number;
}

export interface CompareResult {
  query: string;
  ranked: RankedDocument[];
}

export const embeddingsApi = {
  embed: async (text: string): Promise<EmbeddingResult> => {
    const { data } = await api.post<ApiResponse<EmbeddingResult>>('/ai/embeddings', { text });
    return unwrap(data);
  },

  similarity: async (text1: string, text2: string): Promise<SimilarityResult> => {
    const { data } = await api.post<ApiResponse<SimilarityResult>>('/ai/embeddings/similarity', {
      text1,
      text2,
    });
    return unwrap(data);
  },

  batch: async (texts: string[]): Promise<BatchEmbeddingResult> => {
    const { data } = await api.post<ApiResponse<BatchEmbeddingResult>>('/ai/embeddings/batch', {
      texts,
    });
    return unwrap(data);
  },

  compare: async (query: string, documents: string[]): Promise<CompareResult> => {
    const { data } = await api.post<ApiResponse<CompareResult>>('/ai/embeddings/compare', {
      query,
      documents,
    });
    return unwrap(data);
  },
};
