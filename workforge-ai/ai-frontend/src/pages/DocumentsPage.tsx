import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { demoApi } from '@/api/demoApi';
import { documentsApi, type DocumentSearchHit } from '@/api/documentsApi';
import { formatApiError } from '@/api/client';
import { ApiErrorBanner } from '@/components/ApiErrorBanner';
import { AppNav } from '@/components/AppNav';
import { DocumentForm } from '@/components/DocumentForm';
import { DocumentList } from '@/components/DocumentList';
import { SemanticSearch } from '@/components/SemanticSearch';
import { SampleChips } from '@/components/SampleChips';

const SEARCH_SAMPLES = [
  'What is a sprint?',
  'What is RAG?',
  'What is MCP?',
  'What is a backlog?',
];

export default function DocumentsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [searchHits, setSearchHits] = useState<DocumentSearchHit[] | null>(null);
  const [seedSummary, setSeedSummary] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('What is a sprint?');

  const listQuery = useQuery({
    queryKey: ['documents', page],
    queryFn: () => documentsApi.list(page, 10),
  });

  const createMutation = useMutation({
    mutationFn: ({ content, metadata }: { content: string; metadata: Record<string, unknown> }) =>
      documentsApi.create(content, metadata),
    onSuccess: () => {
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => documentsApi.remove(id),
    onSuccess: () => {
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const searchMutation = useMutation({
    mutationFn: ({ query, topK }: { query: string; topK: number }) => documentsApi.search(query, topK),
    onSuccess: (data) => {
      setSearchHits(data.results);
      setError(null);
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const seedMutation = useMutation({
    mutationFn: () => demoApi.seed(),
    onSuccess: (data) => {
      setError(null);
      setSeedSummary(
        `Created ${data.documentsCreated}, skipped ${data.documentsSkipped}. Topics: ${data.documentTopics.join(', ')}`,
      );
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const busy =
    createMutation.isPending ||
    deleteMutation.isPending ||
    searchMutation.isPending ||
    seedMutation.isPending;

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col gap-6 px-4 py-6">
      <AppNav />
      <header className="rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-slate-50">PGVector documents</h1>
            <p className="mt-1 text-sm text-muted">
              Phase 4 — ingest text, store Ollama embeddings in PostgreSQL, and run semantic topK search.
            </p>
          </div>
          <button
            type="button"
            disabled={busy}
            onClick={() => seedMutation.mutate()}
            className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
          >
            {seedMutation.isPending ? 'Seeding…' : 'Load demo documents'}
          </button>
        </div>
        {seedSummary && <p className="mt-3 text-xs text-muted">{seedSummary}</p>}
      </header>

      <ApiErrorBanner
        error={error ?? (listQuery.isError ? listQuery.error : null)}
        onRetry={() => {
          setError(null);
          void listQuery.refetch();
        }}
      />

      <DocumentForm
        disabled={busy}
        onSubmit={(content, metadata) => createMutation.mutate({ content, metadata })}
      />

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <SampleChips
          label="Search samples"
          samples={SEARCH_SAMPLES}
          disabled={busy}
          onSelect={(q) => {
            setSearchQuery(q);
            searchMutation.mutate({ query: q, topK: 5 });
          }}
        />
        <SemanticSearch
          busy={busy}
          results={searchHits}
          query={searchQuery}
          onSearch={(query, topK) => {
            setSearchQuery(query);
            searchMutation.mutate({ query, topK });
          }}
        />
      </section>

      {listQuery.isLoading && <p className="text-sm text-muted">Loading documents…</p>}
      {!listQuery.isLoading && (listQuery.data?.totalElements ?? 0) === 0 && (
        <p className="text-sm text-muted">No documents yet. Click “Load demo documents” to seed knowledge.</p>
      )}

      <DocumentList
        items={listQuery.data?.items ?? []}
        totalElements={listQuery.data?.totalElements ?? 0}
        page={listQuery.data?.page ?? page}
        totalPages={listQuery.data?.totalPages ?? 0}
        busy={busy || listQuery.isFetching}
        onDelete={(id) => deleteMutation.mutate(id)}
        onPrev={() => setPage((p) => Math.max(0, p - 1))}
        onNext={() => setPage((p) => p + 1)}
      />
    </div>
  );
}
