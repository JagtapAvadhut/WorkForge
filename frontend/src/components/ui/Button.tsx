import { forwardRef, type ButtonHTMLAttributes } from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '@/utils/cn';

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'outline';
type Size = 'sm' | 'md' | 'lg' | 'icon';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const variants: Record<Variant, string> = {
  primary:
    'bg-accent text-accent-contrast hover:bg-accent-hover shadow-sm focus-visible:ring-accent/40',
  secondary:
    'bg-surface text-content border border-border-strong hover:bg-canvas focus-visible:ring-accent/30',
  outline:
    'bg-transparent text-content border border-border-strong hover:bg-surface focus-visible:ring-accent/30',
  ghost: 'bg-transparent text-content-muted hover:bg-surface hover:text-content',
  danger: 'bg-danger text-white hover:bg-danger/90 shadow-sm focus-visible:ring-danger/40',
};

const sizes: Record<Size, string> = {
  sm: 'h-8 px-3 text-xs gap-1.5',
  md: 'h-9 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-sm gap-2',
  icon: 'h-9 w-9 p-0',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', loading, disabled, children, ...props }, ref) => {
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={cn(
          'inline-flex select-none items-center justify-center rounded-md font-medium transition-colors',
          'focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-canvas',
          'disabled:cursor-not-allowed disabled:opacity-60',
          variants[variant],
          sizes[size],
          className,
        )}
        {...props}
      >
        {loading ? <Loader2 className="animate-spin" size={16} aria-hidden /> : null}
        {children}
      </button>
    );
  },
);
Button.displayName = 'Button';
