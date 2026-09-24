import { api, unwrap } from './client';
import type { ApiResponse, AuthResult, AuthTokens, User } from '@/types';

export interface LoginPayload {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  username: string;
  fullName: string;
  password: string;
}

/** Flat auth payload returned by the WorkForge backend. */
interface BackendAuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
  user: User;
}

interface BackendMeResponse {
  user: User;
  permissions?: string[];
}

function toAuthResult(raw: BackendAuthResponse): AuthResult {
  return {
    user: raw.user,
    tokens: {
      accessToken: raw.accessToken,
      refreshToken: raw.refreshToken,
      tokenType: raw.tokenType,
      expiresIn: raw.expiresIn,
    },
  };
}

export const authApi = {
  login: async (payload: LoginPayload): Promise<AuthResult> => {
    const raw = await api
      .post<ApiResponse<BackendAuthResponse>>('/auth/login', {
        usernameOrEmail: payload.usernameOrEmail,
        password: payload.password,
      })
      .then(unwrap);
    return toAuthResult(raw);
  },

  register: async (payload: RegisterPayload): Promise<AuthResult> => {
    const raw = await api
      .post<ApiResponse<BackendAuthResponse>>('/auth/register', payload)
      .then(unwrap);
    return toAuthResult(raw);
  },

  refresh: async (refreshToken: string): Promise<AuthTokens> => {
    const raw = await api
      .post<ApiResponse<BackendAuthResponse>>('/auth/refresh', { refreshToken })
      .then(unwrap);
    return {
      accessToken: raw.accessToken,
      refreshToken: raw.refreshToken,
      tokenType: raw.tokenType,
      expiresIn: raw.expiresIn,
    };
  },

  logout: (refreshToken?: string | null) =>
    api.post<ApiResponse<null>>('/auth/logout', { refreshToken }).then(unwrap),

  me: async (): Promise<User> => {
    const raw = await api.get<ApiResponse<BackendMeResponse | User>>('/auth/me').then(unwrap);
    if (raw && typeof raw === 'object' && 'user' in raw && (raw as BackendMeResponse).user) {
      return (raw as BackendMeResponse).user;
    }
    return raw as User;
  },
};
