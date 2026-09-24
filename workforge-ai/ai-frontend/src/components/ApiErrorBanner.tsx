import { formatApiError } from '@/api/client';

interface Props {
  error: unknown;
  onRetry?: () => void;
}

export function ApiErrorBanner({ error, onRetry }: Props) {
  if (error == null) return null;
  const message = typeof error === 'string' ? error : formatApiError(error);
  return (
    <div className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">
      <p>{message}</p>
      {onRetry && (
        <button
          type="button"
          className="mt-2 rounded-lg border border-border px-3 py-1 text-xs text-slate-100"
          onClick={onRetry}
        >
          Retry
        </button>
      )}
    </div>
  );
}
