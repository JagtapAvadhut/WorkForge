import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { GlobalSearch } from '@/components/common/GlobalSearch';
import { NotificationBell } from '@/components/common/NotificationBell';
import { UserMenu } from '@/components/common/UserMenu';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { useUiStore } from '@/stores/uiStore';

export function TopBar() {
  const openCreateIssue = useUiStore((s) => s.openCreateIssue);

  return (
    <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-border bg-canvas/80 px-4 backdrop-blur">
      <div className="flex flex-1 items-center gap-3">
        <GlobalSearch />
      </div>

      <div className="flex items-center gap-2">
        <Button size="sm" onClick={() => openCreateIssue()} className="hidden sm:inline-flex">
          <Plus size={16} />
          Create
        </Button>
        <Button size="icon" variant="ghost" onClick={() => openCreateIssue()} className="sm:hidden" aria-label="Create issue">
          <Plus size={18} />
        </Button>
        <div className="hidden md:block">
          <ThemeToggle />
        </div>
        <NotificationBell />
        <div className="mx-1 hidden h-6 w-px bg-border sm:block" />
        <UserMenu />
      </div>
    </header>
  );
}
