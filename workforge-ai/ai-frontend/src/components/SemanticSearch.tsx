import { useEffect, useState } from 'react';
import type { DocumentSearchHit } from '@/api/documentsApi';

interface Props {
  busy?: boolean;
  results: DocumentSearchHit[] | null;
  query?: string;
  onSearch: (query: string, topK: number) => void;
}

export function SemanticSearch({ busy, results, query: externalQuery, onSearch }: Props) {
  const [query, setQuery] = useState(externalQuery ?? 'What is a sprint?');
  const [topK, setTopK] = useState(5);

  useEffect(() => {
    if (externalQuery != null) setQuery(externalQuery);
  }, [externalQuery]);

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-medium text-slate-100">Semantic search</h2>
      <div className="flex flex-col gap-3 md:flex-row">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="flex-1 rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
          placeholder="Search query"
        />
        <input
          type="number"
          min={1}
          max={20}
          value={topK}
          onChange={(e) => setTopK(Number(e.target.value))}
          className="w-24 rounded-xl border border-border bg-canvas px-3 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
          title="topK"
        />
        <button
          type="button"
          disabled={busy || !query.trim()}
          onClick={() => onSearch(query.trim(), topK)}
          className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
        >
          {busy ? 'Searching…' : 'Search'}
        </button>
      </div>

      {results && (
        <ol className="space-y-2">
          {results.length === 0 ? (
            <p className="text-sm text-muted">No matches (empty store or low similarity).</p>
          ) : (
            results.map((hit, index) => (
              <li key={hit.id} className="rounded-xl border border-border bg-canvas/60 px-4 py-3 text-sm">
                <span className="font-mono text-xs text-accent">
                  #{index + 1} · similarity {hit.similarity.toFixed(4)}
                </span>
                <p className="mt-1 text-slate-100">{hit.content}</p>
                <p className="mt-1 font-mono text-[11px] text-muted">{JSON.stringify(hit.metadata)}</p>
              </li>
            ))
          )}
        </ol>
      )}
    </div>
  );
}
