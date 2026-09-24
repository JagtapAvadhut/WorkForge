import axios, { AxiosError } from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

/** Default for normal AI requests (chat/rag/agent). Long eval uses async polling. */
export const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 90_000,
});

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}

export class ApiError extends Error {
  status?: number;
  traceId?: string;

  constructor(message: string, status?: number, traceId?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.traceId = traceId;
  }
}

export function unwrap<T>(payload: ApiResponse<T>): T {
  if (!payload.success) {
    throw new ApiError(payload.message || 'Request failed');
  }
  return payload.data;
}

export function formatApiError(err: unknown): string {
  if (err instanceof ApiError) {
    const bits = [err.message];
    if (err.status) bits.push(`HTTP ${err.status}`);
    if (err.traceId) bits.push(`traceId=${err.traceId}`);
    return bits.join(' · ');
  }
  if (axios.isAxiosError(err)) {
    const ax = err as AxiosError<ApiResponse<unknown>>;
    if (ax.code === 'ECONNABORTED') {
      return 'Request timed out. For long evaluation runs, use async suite polling (SMOKE/CORE/ALL).';
    }
    const msg = ax.response?.data?.message || ax.message || 'Network request failed';
    const status = ax.response?.status;
    return status ? `${msg} (HTTP ${status})` : msg;
  }
  if (err instanceof Error) return err.message;
  return 'Request failed';
}

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const message =
      error.response?.data?.message ||
      (error.code === 'ECONNABORTED'
        ? 'Request timed out'
        : error.message || 'Request failed');
    const status = error.response?.status;
    return Promise.reject(new ApiError(message, status));
  },
);
