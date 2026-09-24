import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { embeddingsApi, type EmbeddingResult, type RankedDocument } from '@/api/embeddingsApi';
import { formatApiError } from '@/api/client';
import { ApiErrorBanner } from '@/components/ApiErrorBanner';
import { AppNav } from '@/components/AppNav';
import { SampleChips } from '@/components/SampleChips';

const PREVIEW_N = 8;

const DEMO = {
  single: 'What is a sprint?',
  text1: 'How do I create a sprint?',
  text2: 'How can I start a new sprint?',
  query: 'How do I create a sprint?',
  documents:
    'A sprint is a fixed development period.\nA backlog contains planned work.\nA bug describes incorrect system behavior.',
};

function previewVector(values: number[], n = PREVIEW_N): string {
  const slice = values.slice(0, n).map((v) => v.toFixed(4));
  const more = values.length > n ? `, … (+${values.length - n} more)` : '';
  return `[${slice.join(', ')}${more}]`;
}

export default function EmbeddingsPage() {
  const [singleText, setSingleText] = useState(DEMO.single);
  const [text1, setText1] = useState(DEMO.text1);
  const [text2, setText2] = useState(DEMO.text2);
  const [query, setQuery] = useState(DEMO.query);
  const [documents, setDocuments] = useState(DEMO.documents);

  const [embedResult, setEmbedResult] = useState<EmbeddingResult | null>(null);
  const [similarity, setSimilarity] = useState<number | null>(null);
  const [ranked, setRanked] = useState<RankedDocument[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const embedMutation = useMutation({
    mutationFn: () => embeddingsApi.embed(singleText),
    onSuccess: (data) => {
      setEmbedResult(data);
      setError(null);
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const similarityMutation = useMutation({
    mutationFn: () => embeddingsApi.similarity(text1, text2),
    onSuccess: (data) => {
      setSimilarity(data.similarity);
      setError(null);
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const compareMutation = useMutation({
    mutationFn: () => {
      const docs = documents
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean);
      return embeddingsApi.compare(query, docs);
    },
    onSuccess: (data) => {
      setRanked(data.ranked);
      setError(null);
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const busy = embedMutation.isPending || similarityMutation.isPending || compareMutation.isPending;

  function loadDemo() {
    setSingleText(DEMO.single);
    setText1(DEMO.text1);
    setText2(DEMO.text2);
    setQuery(DEMO.query);
    setDocuments(DEMO.documents);
    setError(null);
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col px-4 py-6">
      <AppNav />

      <header className="mb-6 rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-slate-50">Embeddings lab</h1>
            <p className="mt-1 text-sm text-muted">
              Phase 3 — local Ollama embedding vectors, cosine similarity, and ranked semantic compare.
            </p>
          </div>
          <button
            type="button"
            onClick={loadDemo}
            className="rounded-xl border border-border px-4 py-2 text-sm text-slate-100"
          >
            Load Demo
          </button>
        </div>
      </header>

      <ApiErrorBanner error={error} onRetry={() => setError(null)} />

      <section className="mb-6 space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <h2 className="text-lg font-medium text-slate-100">1. Single text embedding</h2>
        <SampleChips
          samples={[DEMO.single, 'What is RAG?', 'Explain embeddings with an example.']}
          disabled={busy}
          onSelect={setSingleText}
        />
        <textarea
          value={singleText}
          onChange={(e) => setSingleText(e.target.value)}
          rows={3}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <button
          type="button"
          disabled={busy || !singleText.trim()}
          onClick={() => embedMutation.mutate()}
          className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
        >
          {embedMutation.isPending ? 'Embedding…' : 'Embed'}
        </button>
        {embedResult && (
          <div className="rounded-xl border border-border bg-canvas/60 p-4 font-mono text-xs text-slate-200">
            <p>dimensions: {embedResult.dimensions}</p>
            <p className="mt-2 break-all">preview: {previewVector(embedResult.embedding)}</p>
          </div>
        )}
      </section>

      <section className="mb-6 space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <h2 className="text-lg font-medium text-slate-100">2. Compare two texts</h2>
        <div className="grid gap-3 md:grid-cols-2">
          <textarea
            value={text1}
            onChange={(e) => setText1(e.target.value)}
            rows={3}
            className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
          />
          <textarea
            value={text2}
            onChange={(e) => setText2(e.target.value)}
            rows={3}
            className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
          />
        </div>
        <button
          type="button"
          disabled={busy || !text1.trim() || !text2.trim()}
          onClick={() => similarityMutation.mutate()}
          className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
        >
          {similarityMutation.isPending ? 'Comparing…' : 'Cosine similarity'}
        </button>
        {similarity !== null && (
          <p className="font-mono text-sm text-accent">similarity: {similarity.toFixed(4)}</p>
        )}
      </section>

      <section className="mb-6 space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <h2 className="text-lg font-medium text-slate-100">3. Query vs documents</h2>
        <label className="block text-xs uppercase tracking-wide text-muted">Query</label>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <label className="block text-xs uppercase tracking-wide text-muted">
          Documents (one per line)
        </label>
        <textarea
          value={documents}
          onChange={(e) => setDocuments(e.target.value)}
          rows={5}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <button
          type="button"
          disabled={busy || !query.trim() || !documents.trim()}
          onClick={() => compareMutation.mutate()}
          className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
        >
          {compareMutation.isPending ? 'Ranking…' : 'Rank by similarity'}
        </button>
        {ranked && (
          <ol className="space-y-2">
            {ranked.map((item, index) => (
              <li
                key={`${item.text}-${index}`}
                className="rounded-xl border border-border bg-canvas/60 px-4 py-3 text-sm"
              >
                <span className="font-mono text-xs text-accent">
                  #{index + 1} · {item.similarity.toFixed(4)}
                </span>
                <p className="mt-1 text-slate-100">{item.text}</p>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  );
}
