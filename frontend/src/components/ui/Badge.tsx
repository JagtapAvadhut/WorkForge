import type { HTMLAttributes } from 'react';
import { cn } from '@/utils/cn';

type Tone = 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'info';

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: Tone;
}

const tones: Record<Tone, string> = {
  neutral: 'bg-slate-500/10 text-content-muted ring-1 ring-inset ring-border',
  accent: 'bg-accent/10 text-accent ring-1 ring-inset ring-accent/25',
  success: 'bg-success/10 text-success ring-1 ring-inset ring-success/25',
  warning: 'bg-warning/10 text-warning ring-1 ring-inset ring-warning/25',
  danger: 'bg-danger/10 text-danger ring-1 ring-inset ring-danger/25',
  info: 'bg-info/10 text-info ring-1 ring-inset ring-info/25',
};

export function Badge({ className, tone = 'neutral', ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium',
        tones[tone],
        className,
      )}
      {...props}
    />
  );
}
