import { cn } from '@/utils/cn';

interface LogoProps {
  className?: string;
  size?: number;
  showWordmark?: boolean;
}

export function Logo({ className, size = 28, showWordmark = true }: LogoProps) {
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <span
        className="flex items-center justify-center rounded-md bg-accent text-accent-contrast shadow-sm"
        style={{ width: size, height: size }}
        aria-hidden
      >
        <svg viewBox="0 0 32 32" width={size * 0.66} height={size * 0.66} fill="none">
          <path
            d="M6 8h4l2.4 10L15 8h2.6l2.6 10L22.6 8h4l-4.2 16h-4L16 13.6 13.6 24h-4z"
            fill="currentColor"
          />
        </svg>
      </span>
      {showWordmark ? (
        <span className="text-[15px] font-bold tracking-tight text-content">
          Work<span className="text-accent">Forge</span>
        </span>
      ) : null}
    </span>
  );
}
