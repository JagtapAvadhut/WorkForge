import { useMemo } from 'react';
import { cn } from '@/utils/cn';
import { initialsOf } from '@/utils/format';
import type { User } from '@/types';

interface AvatarProps {
  user?: Pick<User, 'fullName' | 'displayName' | 'avatarUrl' | 'username'> | null;
  name?: string;
  size?: number;
  className?: string;
}

// Deterministic, muted palette (no purple gradients).
const PALETTE = [
  'bg-teal-600',
  'bg-sky-600',
  'bg-slate-600',
  'bg-emerald-600',
  'bg-cyan-700',
  'bg-blue-700',
  'bg-indigo-600',
  'bg-rose-600',
];

function colorFor(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) hash = (hash * 31 + seed.charCodeAt(i)) >>> 0;
  return PALETTE[hash % PALETTE.length];
}

export function Avatar({ user, name, size = 28, className }: AvatarProps) {
  const displayName = user?.displayName || user?.fullName || name || user?.username || '';
  const initials = useMemo(() => initialsOf(displayName), [displayName]);
  const color = useMemo(() => colorFor(displayName || 'x'), [displayName]);

  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full font-semibold text-white ring-1 ring-black/5',
        !user?.avatarUrl && color,
        className,
      )}
      style={{ width: size, height: size, fontSize: Math.max(10, size * 0.4) }}
      title={displayName || undefined}
      aria-label={displayName || 'User'}
    >
      {user?.avatarUrl ? (
        <img src={user.avatarUrl} alt="" className="h-full w-full object-cover" />
      ) : (
        initials
      )}
    </span>
  );
}
