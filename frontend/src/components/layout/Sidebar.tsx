import { NavLink } from 'react-router-dom';
import {
  Bell,
  ChevronLeft,
  Filter,
  FolderKanban,
  Gauge,
  KanbanSquare,
  LayoutDashboard,
  ListTodo,
  ListChecks,
  Repeat,
  ShieldCheck,
  UserRound,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { Logo } from '@/components/common/Logo';
import { useUiStore } from '@/stores/uiStore';
import { usePermissions } from '@/hooks/usePermissions';
import { cn } from '@/utils/cn';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
}

const primaryNav: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/projects', label: 'Projects', icon: FolderKanban },
  { to: '/my-work', label: 'My Work', icon: ListChecks },
  { to: '/issues', label: 'Issues', icon: ListTodo },
];

const workNav: NavItem[] = [
  { to: '/backlog', label: 'Backlog', icon: KanbanSquare },
  { to: '/board', label: 'Board', icon: Gauge },
  { to: '/sprints', label: 'Sprints', icon: Repeat },
  { to: '/reports', label: 'Reports', icon: Gauge },
];

const utilityNav: NavItem[] = [
  { to: '/filters', label: 'Filters', icon: Filter },
  { to: '/notifications', label: 'Notifications', icon: Bell },
];

export function Sidebar() {
  const collapsed = useUiStore((s) => s.sidebarCollapsed);
  const toggle = useUiStore((s) => s.toggleSidebar);
  const { isAdmin } = usePermissions();

  return (
    <aside
      className={cn(
        'sticky top-0 z-30 flex h-screen shrink-0 flex-col border-r border-border bg-surface/70 backdrop-blur transition-[width] duration-200',
        collapsed ? 'w-16' : 'w-60',
      )}
    >
      <div className="flex h-14 items-center justify-between px-3">
        {collapsed ? (
          <Logo showWordmark={false} className="mx-auto" />
        ) : (
          <Logo />
        )}
      </div>

      <nav className="wf-scrollbar flex-1 space-y-6 overflow-y-auto px-2.5 py-3">
        <NavGroup items={primaryNav} collapsed={collapsed} />
        <NavGroup label="Planning" items={workNav} collapsed={collapsed} />
        <NavGroup label="Personal" items={utilityNav} collapsed={collapsed} />
        {isAdmin ? (
          <NavGroup
            label="Administration"
            collapsed={collapsed}
            items={[
              { to: '/admin/users', label: 'Users', icon: UserRound },
              { to: '/admin/roles', label: 'Roles', icon: ShieldCheck },
              { to: '/admin/settings', label: 'Settings', icon: Gauge },
            ]}
          />
        ) : null}
      </nav>

      <button
        type="button"
        onClick={toggle}
        className="m-2.5 flex h-9 items-center justify-center gap-2 rounded-md border border-border text-content-subtle transition-colors hover:bg-canvas hover:text-content"
        aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
      >
        <ChevronLeft size={16} className={cn('transition-transform', collapsed && 'rotate-180')} />
        {!collapsed ? <span className="text-sm">Collapse</span> : null}
      </button>
    </aside>
  );
}

function NavGroup({
  label,
  items,
  collapsed,
}: {
  label?: string;
  items: NavItem[];
  collapsed: boolean;
}) {
  return (
    <div>
      {label && !collapsed ? (
        <p className="mb-1.5 px-2.5 text-[11px] font-semibold uppercase tracking-wide text-content-subtle">
          {label}
        </p>
      ) : null}
      <ul className="space-y-0.5">
        {items.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              end={item.end}
              title={collapsed ? item.label : undefined}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium transition-colors',
                  collapsed && 'justify-center px-0',
                  isActive
                    ? 'bg-accent/10 text-accent'
                    : 'text-content-muted hover:bg-canvas hover:text-content',
                )
              }
            >
              <item.icon size={18} className="shrink-0" />
              {!collapsed ? <span className="truncate">{item.label}</span> : null}
            </NavLink>
          </li>
        ))}
      </ul>
    </div>
  );
}
