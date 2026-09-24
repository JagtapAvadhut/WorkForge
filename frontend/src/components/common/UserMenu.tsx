import { useNavigate } from 'react-router-dom';
import { ChevronDown, LogOut, Settings, ShieldCheck, UserCog } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { DropdownMenu, MenuItem, MenuSeparator } from '@/components/ui/DropdownMenu';
import { useAuthStore } from '@/stores/authStore';
import { useLogout } from '@/hooks/useAuth';
import { usePermissions } from '@/hooks/usePermissions';

export function UserMenu() {
  const user = useAuthStore((s) => s.user);
  const logout = useLogout();
  const navigate = useNavigate();
  const { isAdmin } = usePermissions();

  if (!user) return null;

  return (
    <DropdownMenu
      trigger={
        <span className="flex items-center gap-2 rounded-md py-1 pl-1 pr-2 transition-colors hover:bg-surface">
          <Avatar user={user} size={30} />
          <span className="hidden text-left sm:block">
            <span className="block max-w-[10rem] truncate text-sm font-medium text-content">
              {user.fullName}
            </span>
            <span className="block text-xs text-content-subtle">{user.roles?.[0] ?? 'Member'}</span>
          </span>
          <ChevronDown size={15} className="text-content-subtle" />
        </span>
      }
    >
      {(close) => (
        <>
          <div className="px-2.5 py-2">
            <p className="text-sm font-semibold text-content">{user.fullName}</p>
            <p className="truncate text-xs text-content-subtle">{user.email}</p>
          </div>
          <MenuSeparator />
          <MenuItem
            icon={<UserCog size={15} />}
            onClick={() => {
              navigate('/my-work');
              close();
            }}
          >
            My work
          </MenuItem>
          {isAdmin ? (
            <>
              <MenuItem
                icon={<ShieldCheck size={15} />}
                onClick={() => {
                  navigate('/admin/users');
                  close();
                }}
              >
                Administration
              </MenuItem>
              <MenuItem
                icon={<Settings size={15} />}
                onClick={() => {
                  navigate('/admin/settings');
                  close();
                }}
              >
                System settings
              </MenuItem>
            </>
          ) : null}
          <MenuSeparator />
          <MenuItem
            icon={<LogOut size={15} />}
            danger
            onClick={() => {
              close();
              void logout().then(() => navigate('/login'));
            }}
          >
            Sign out
          </MenuItem>
        </>
      )}
    </DropdownMenu>
  );
}
