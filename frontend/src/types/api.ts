/** Consistent API response envelope returned by the WorkForge backend. */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  error: ApiError | null;
  timestamp: string;
  traceId: string;
}

export interface ApiError {
  code: string;
  message: string;
  /** Field-level validation errors keyed by field name. */
  fieldErrors?: Record<string, string>;
  details?: unknown;
}

/** Standard Spring-style paginated payload. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
}
