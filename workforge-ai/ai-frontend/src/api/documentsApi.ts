import { api, unwrap, type ApiResponse } from './client';

export interface DocumentResponse {
  id: string;
  content: string;
  metadata: Record<string, unknown>;
  embeddingDimensions: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentPageResponse {
  items: DocumentResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DocumentSearchHit {
  id: string;
  content: string;
  metadata: Record<string, unknown>;
  similarity: number;
}

export interface DocumentSearchResponse {
  query: string;
  topK: number;
  results: DocumentSearchHit[];
}

export const documentsApi = {
  create: async (content: string, metadata: Record<string, unknown>): Promise<DocumentResponse> => {
    const { data } = await api.post<ApiResponse<DocumentResponse>>('/ai/documents', {
      content,
      metadata,
    });
    return unwrap(data);
  },

  list: async (page = 0, size = 20): Promise<DocumentPageResponse> => {
    const { data } = await api.get<ApiResponse<DocumentPageResponse>>('/ai/documents', {
      params: { page, size },
    });
    return unwrap(data);
  },

  remove: async (id: string): Promise<void> => {
    const { data } = await api.delete<ApiResponse<null>>(`/ai/documents/${id}`);
    unwrap(data);
  },

  search: async (query: string, topK: number): Promise<DocumentSearchResponse> => {
    const { data } = await api.post<ApiResponse<DocumentSearchResponse>>('/ai/documents/search', {
      query,
      topK,
    });
    return unwrap(data);
  },
};
