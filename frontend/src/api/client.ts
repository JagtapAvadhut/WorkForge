import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import type { ApiResponse, AuthTokens } from '@/types';
import { authTokens } from '@/stores/authStore';
import { env } from '@/utils/env';

/** Normalised error thrown to callers / react-query. */
export class ApiRequestError extends Error {
  status: number;
  code: string;
  fieldErrors?: Record<string, string>;

  constructor(message: string, status: number, code: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export const api: AxiosInstance = axios.create({
  baseURL: env.apiBaseUrl,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
});

// A bare client (no interceptors) used exclusively for the refresh call to
// avoid infinite interceptor recursion.
const refreshClient: AxiosInstance = axios.create({
  baseURL: env.apiBaseUrl,
  headers: { 'Content-Type': 'application/json' },
});

// ---- Request interceptor: attach bearer token ----
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = authTokens.getAccess();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// ---- Response interceptor: transparent refresh on 401 ----
interface RetriableConfig extends AxiosRequestConfig {
  _retry?: boolean;
}

let refreshPromise: Promise<AuthTokens> | null = null;

async function runRefresh(): Promise<AuthTokens> {
  const refreshToken = authTokens.getRefresh();
  if (!refreshToken) throw new Error('No refresh token');
  const { data } = await refreshClient.post<
    ApiResponse<{ accessToken: string; refreshToken: string; tokenType?: string; expiresIn?: number }>
  >('/auth/refresh', { refreshToken });
  if (!data.success || !data.data?.accessToken) throw new Error('Refresh failed');
  const tokens: AuthTokens = {
    accessToken: data.data.accessToken,
    refreshToken: data.data.refreshToken,
    tokenType: data.data.tokenType,
    expiresIn: data.data.expiresIn,
  };
  authTokens.set(tokens);
  return tokens;
}

function onLoggedOut() {
  authTokens.clear();
  if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search);
    window.location.assign(`/login?redirect=${redirect}`);
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as (RetriableConfig & InternalAxiosRequestConfig) | undefined;
    const status = error.response?.status;
    const url = original?.url ?? '';

    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/refresh');

    if (status === 401 && original && !original._retry && !isAuthCall && authTokens.getRefresh()) {
      original._retry = true;
      try {
        refreshPromise = refreshPromise ?? runRefresh();
        const tokens = await refreshPromise;
        refreshPromise = null;
        original.headers.set('Authorization', `Bearer ${tokens.accessToken}`);
        return api(original);
      } catch (refreshError) {
        refreshPromise = null;
        onLoggedOut();
        return Promise.reject(normalizeError(refreshError));
      }
    }

    return Promise.reject(normalizeError(error));
  },
);

export function normalizeError(error: unknown): ApiRequestError {
  if (axios.isAxiosError(error)) {
    const axErr = error as AxiosError<ApiResponse<unknown>>;
    const status = axErr.response?.status ?? 0;
    const body = axErr.response?.data;
    const apiError = body?.error;
    return new ApiRequestError(
      apiError?.message || body?.message || axErr.message || 'Request failed',
      status,
      apiError?.code || (status === 0 ? 'NETWORK_ERROR' : 'REQUEST_ERROR'),
      apiError?.fieldErrors,
    );
  }
  if (error instanceof Error) {
    return new ApiRequestError(error.message, 0, 'UNKNOWN');
  }
  return new ApiRequestError('Unknown error', 0, 'UNKNOWN');
}

/** Unwrap the standard API envelope, throwing on `success: false`. */
export function unwrap<T>(response: { data: ApiResponse<T> }): T {
  const { data } = response;
  if (!data.success) {
    throw new ApiRequestError(
      data.error?.message || data.message || 'Request failed',
      200,
      data.error?.code || 'API_ERROR',
      data.error?.fieldErrors,
    );
  }
  return data.data;
}
