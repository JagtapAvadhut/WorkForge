import { AlertTriangle } from 'lucide-react';
import { Button } from './Button';
import { cn } from '@/utils/cn';

interface ErrorStateProps {
  title?: string;
  error?: unknown;
  onRetry?: () => void;
  className?: string;
}

function messageOf(error: unknown): string {
  if (!error) return 'Something went wrong.';
  if (typeof error === 'string') return error;
  if (error instanceof Error) return error.message;
  return 'An unexpected error occurred.';
}

export function ErrorState({ title = 'Unable to load data', error, onRetry, className }: ErrorStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center rounded-lg border border-danger/30 bg-danger/5 px-6 py-10 text-center',
        className,
      )}
    >
      <span className="mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-danger/10 text-danger">
        <AlertTriangle size={20} aria-hidden />
      </span>
      <h3 className="text-sm font-semibold text-content">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-content-muted">{messageOf(error)}</p>
      {onRetry ? (
        <Button variant="secondary" size="sm" className="mt-4" onClick={onRetry}>
          Try again
        </Button>
      ) : null}
    </div>
  );
}
