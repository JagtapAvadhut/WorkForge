import { api, unwrap } from './client';
import type { ApiResponse, SearchResults } from '@/types';

export const searchApi = {
  global: (query: string) =>
    api
      .get<ApiResponse<SearchResults>>('/search', { params: { q: query } })
      .then(unwrap),
};
