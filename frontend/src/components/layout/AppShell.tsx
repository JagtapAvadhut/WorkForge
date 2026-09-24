import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { CreateIssueModal } from '@/components/issues/CreateIssueModal';
import { useUiStore } from '@/stores/uiStore';

export function AppShell() {
  const createIssueOpen = useUiStore((s) => s.createIssueOpen);
  const createIssueDefaults = useUiStore((s) => s.createIssueDefaults);
  const closeCreateIssue = useUiStore((s) => s.closeCreateIssue);

  return (
    <div className="flex min-h-screen w-full">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar />
        <main className="wf-scrollbar flex-1 overflow-x-hidden px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto w-full max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>

      <CreateIssueModal
        open={createIssueOpen}
        onClose={closeCreateIssue}
        defaultProjectKey={createIssueDefaults?.projectKey}
        defaultSprintId={createIssueDefaults?.sprintId}
      />
    </div>
  );
}
