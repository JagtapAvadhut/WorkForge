import { useCallback, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authApi, type LoginPayload, type RegisterPayload } from '@/api/authApi';
import { useAuthStore } from '@/stores/authStore';

/**
 * Bootstraps the session on app load: if we have a persisted token, fetch the
 * current user. Marks the store as initialized either way.
 */
export function useAuthBootstrap() {
  const { accessToken, user, setUser, setInitialized, clear, initialized } = useAuthStore();

  useEffect(() => {
    let cancelled = false;
    async function boot() {
      if (!accessToken) {
        setInitialized(true);
        return;
      }
      if (user) {
        setInitialized(true);
        return;
      }
      try {
        const me = await authApi.me();
        if (!cancelled) setUser(me);
      } catch {
        if (!cancelled) clear();
      } finally {
        if (!cancelled) setInitialized(true);
      }
    }
    void boot();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return initialized;
}

export function useLogin() {
  const setSession = useAuthStore((s) => s.setSession);
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: LoginPayload) => authApi.login(payload),
    onSuccess: (result) => {
      setSession(result);
      void queryClient.invalidateQueries();
    },
  });
}

export function useRegister() {
  const setSession = useAuthStore((s) => s.setSession);
  return useMutation({
    mutationFn: (payload: RegisterPayload) => authApi.register(payload),
    onSuccess: (result) => setSession(result),
  });
}

export function useLogout() {
  const { refreshToken, clear } = useAuthStore();
  const queryClient = useQueryClient();
  return useCallback(async () => {
    try {
      await authApi.logout(refreshToken);
    } catch {
      // best-effort; clear locally regardless
    } finally {
      clear();
      queryClient.clear();
    }
  }, [refreshToken, clear, queryClient]);
}
