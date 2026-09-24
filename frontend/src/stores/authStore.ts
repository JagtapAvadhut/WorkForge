import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthTokens, Role, User } from '@/types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  /** Whether the initial "who am I" bootstrap has completed. */
  initialized: boolean;

  setSession: (payload: { user: User; tokens: AuthTokens }) => void;
  setTokens: (tokens: AuthTokens) => void;
  setUser: (user: User | null) => void;
  setInitialized: (value: boolean) => void;
  clear: () => void;

  isAuthenticated: () => boolean;
  hasRole: (...roles: Role[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      initialized: false,

      setSession: ({ user, tokens }) =>
        set({
          user,
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
        }),
      setTokens: (tokens) =>
        set({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken }),
      setUser: (user) => set({ user }),
      setInitialized: (value) => set({ initialized: value }),
      clear: () => set({ user: null, accessToken: null, refreshToken: null }),

      isAuthenticated: () => Boolean(get().accessToken),
      hasRole: (...roles) => {
        const user = get().user;
        if (!user) return false;
        return roles.some((r) => user.roles?.includes(r));
      },
    }),
    {
      name: 'workforge.auth',
      // Only persist tokens; the user object is refreshed from /auth/me on boot.
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    },
  ),
);

/** Non-hook accessors for use inside the axios layer. */
export const authTokens = {
  getAccess: () => useAuthStore.getState().accessToken,
  getRefresh: () => useAuthStore.getState().refreshToken,
  set: (tokens: AuthTokens) => useAuthStore.getState().setTokens(tokens),
  clear: () => useAuthStore.getState().clear(),
};
