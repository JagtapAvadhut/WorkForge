import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type ThemeMode = 'light' | 'dark' | 'system';

interface UiState {
  theme: ThemeMode;
  sidebarCollapsed: boolean;
  createIssueOpen: boolean;
  createIssueDefaults: { projectKey?: string; sprintId?: string } | null;

  setTheme: (theme: ThemeMode) => void;
  toggleSidebar: () => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  openCreateIssue: (defaults?: { projectKey?: string; sprintId?: string }) => void;
  closeCreateIssue: () => void;
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      theme: 'system',
      sidebarCollapsed: false,
      createIssueOpen: false,
      createIssueDefaults: null,

      setTheme: (theme) => set({ theme }),
      toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      setSidebarCollapsed: (sidebarCollapsed) => set({ sidebarCollapsed }),
      openCreateIssue: (defaults) => set({ createIssueOpen: true, createIssueDefaults: defaults ?? null }),
      closeCreateIssue: () => set({ createIssueOpen: false }),
    }),
    {
      name: 'workforge.ui',
      partialize: (s) => ({ theme: s.theme, sidebarCollapsed: s.sidebarCollapsed }),
    },
  ),
);

/** Resolve the effective theme, honouring the system preference. */
export function resolveTheme(theme: ThemeMode): 'light' | 'dark' {
  if (theme === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return theme;
}
