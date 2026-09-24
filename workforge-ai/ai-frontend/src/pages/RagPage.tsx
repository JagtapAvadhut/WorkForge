import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { ragApi, type RagQueryResult } from '@/api/ragApi';
import { formatApiError } from '@/api/client';
import { AppNav } from '@/components/AppNav';
import { SampleChips } from '@/components/SampleChips';

const RAG_SAMPLES = [
  'What is a sprint?',
  'What is a backlog?',
  'How does a workflow work?',
  'What is RAG?',
  'What is an issue?',
  'What is the internal payroll policy?',
];

export default function RagPage() {
  const [question, setQuestion] = useState('What is a sprint?');
  const [topK, setTopK] = useState(5);
  const [result, setResult] = useState<RagQueryResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => ragApi.query(question.trim(), topK),
    onSuccess: (data) => {
      setResult(data);
      setError(null);
    },
    onError: (err) => setError(formatApiError(err)),
  });

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col gap-6 px-4 py-6">
      <AppNav />

      <header className="rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <h1 className="text-2xl font-semibold text-slate-50">RAG query</h1>
        <p className="mt-1 text-sm text-muted">
          Phase 5 — retrieve PGVector context, then generate a grounded answer with Ollama.
        </p>
      </header>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <SampleChips samples={RAG_SAMPLES} disabled={mutation.isPending} onSelect={setQuestion} />
        <label className="block text-xs uppercase tracking-wide text-muted">Question</label>
        <textarea
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          rows={3}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <div className="flex flex-wrap items-center gap-3">
          <label className="text-xs uppercase tracking-wide text-muted" htmlFor="rag-topk">
            Top-K
          </label>
          <input
            id="rag-topk"
            type="number"
            min={1}
            max={20}
            value={topK}
            onChange={(e) => setTopK(Number(e.target.value))}
            className="w-24 rounded-xl border border-border bg-canvas px-3 py-2 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
          />
          <button
            type="button"
            disabled={mutation.isPending || !question.trim()}
            onClick={() => mutation.mutate()}
            className="rounded-xl bg-accent px-5 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
          >
            {mutation.isPending ? 'Retrieving…' : 'Ask'}
          </button>
        </div>
      </section>

      {error && (
        <div className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          <p>{error}</p>
          <button
            type="button"
            className="mt-2 rounded-lg border border-border px-3 py-1 text-xs"
            onClick={() => mutation.mutate()}
          >
            Retry
          </button>
        </div>
      )}

      {result && (
        <>
          <section className="space-y-2 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Answer</h2>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-100">{result.answer}</p>
          </section>

          <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Sources used</h2>
            {result.sources.length === 0 ? (
              <p className="text-sm text-muted">No retrieved sources (insufficient knowledge).</p>
            ) : (
              <ol className="space-y-3">
                {result.sources.map((source, index) => (
                  <li key={source.id} className="rounded-xl border border-border bg-canvas/60 px-4 py-3 text-sm">
                    <span className="font-mono text-xs text-accent">
                      #{index + 1} · similarity {source.similarity.toFixed(4)}
                    </span>
                    <p className="mt-1 text-slate-100">{source.content}</p>
                    <p className="mt-1 font-mono text-[11px] text-muted">
                      {JSON.stringify(source.metadata)}
                    </p>
                  </li>
                ))}
              </ol>
            )}
          </section>
        </>
      )}
    </div>
  );
}
