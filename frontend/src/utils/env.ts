/** Centralised access to Vite environment variables. */
export const env = {
  apiBaseUrl: (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api/v1',
  isDev: import.meta.env.DEV,
};
