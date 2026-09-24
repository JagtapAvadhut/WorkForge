import type { DocumentResponse } from '@/api/documentsApi';

interface Props {
  items: DocumentResponse[];
  totalElements: number;
  page: number;
  totalPages: number;
  busy?: boolean;
  onDelete: (id: string) => void;
  onPrev: () => void;
  onNext: () => void;
}

export function DocumentList({
  items,
  totalElements,
  page,
  totalPages,
  busy,
  onDelete,
  onPrev,
  onNext,
}: Props) {
  return (
    <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-medium text-slate-100">Documents</h2>
        <span className="font-mono text-xs text-muted">{totalElements} total</span>
      </div>

      {items.length === 0 ? (
        <p className="text-sm text-muted">No documents yet. Add knowledge above.</p>
      ) : (
        <ul className="space-y-3">
          {items.map((doc) => (
            <li key={doc.id} className="rounded-xl border border-border bg-canvas/60 px-4 py-3">
              <p className="text-sm text-slate-100">{doc.content}</p>
              <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-muted">
                <span className="font-mono text-accent">{doc.embeddingDimensions}d</span>
                <span className="font-mono">{JSON.stringify(doc.metadata)}</span>
                <span>{new Date(doc.createdAt).toLocaleString()}</span>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => onDelete(doc.id)}
                  className="ml-auto text-red-300 hover:text-red-200 disabled:opacity-50"
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="flex items-center gap-3 pt-2">
          <button
            type="button"
            disabled={busy || page <= 0}
            onClick={onPrev}
            className="rounded-lg border border-border px-3 py-1 text-sm disabled:opacity-40"
          >
            Prev
          </button>
          <span className="font-mono text-xs text-muted">
            page {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={busy || page + 1 >= totalPages}
            onClick={onNext}
            className="rounded-lg border border-border px-3 py-1 text-sm disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </section>
  );
}
