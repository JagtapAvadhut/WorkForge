import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { graphAgentApi, type GraphAgentResult } from '@/api/graphAgentApi';
import { formatApiError } from '@/api/client';
import { AppNav } from '@/components/AppNav';
import { ConversationPicker } from '@/components/ConversationPicker';
import { SampleChips } from '@/components/SampleChips';

const GRAPH_SAMPLES = [
  'What is a sprint?',
  'Tell me about MWS-1 and its project.',
  'Find open MWS issues.',
  'Investigate MWS-1.',
];

function errorMessage(err: unknown): string {
  return formatApiError(err);
}

export default function GraphAgentPage() {
  const [message, setMessage] = useState('Tell me about MWS-1 and its project.');
  const [maxIterations, setMaxIterations] = useState(5);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [result, setResult] = useState<GraphAgentResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => graphAgentApi.run(message.trim(), maxIterations, conversationId),
    onSuccess: (data) => {
      setResult(data);
      if (data.conversationId) setConversationId(data.conversationId);
      setError(null);
    },
    onError: (err) => setError(errorMessage(err)),
  });

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col gap-6 px-4 py-6">
      <AppNav />

      <header className="rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <h1 className="text-2xl font-semibold text-slate-50">Graph Agent</h1>
        <p className="mt-1 text-sm text-muted">
          Phase 8 + 10 — LangGraph4j with conversation memory. Graph working state remains in-request.
        </p>
      </header>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <ConversationPicker conversationId={conversationId} onChange={setConversationId} />
        <SampleChips samples={GRAPH_SAMPLES} disabled={mutation.isPending} onSelect={setMessage} />
        <label className="block text-xs uppercase tracking-wide text-muted">Request</label>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          rows={3}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <div className="flex flex-wrap items-center gap-3">
          <label className="text-xs uppercase tracking-wide text-muted" htmlFor="graph-max-iter">
            Max iterations
          </label>
          <input
            id="graph-max-iter"
            type="number"
            min={1}
            max={10}
            value={maxIterations}
            onChange={(e) => setMaxIterations(Number(e.target.value))}
            className="w-24 rounded-xl border border-border bg-canvas px-3 py-2 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
          />
          <button
            type="button"
            disabled={mutation.isPending || !message.trim()}
            onClick={() => mutation.mutate()}
            className="rounded-xl bg-accent px-5 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
          >
            {mutation.isPending ? 'Running graph…' : 'Run graph agent'}
          </button>
          <button
            type="button"
            disabled={mutation.isPending}
            onClick={() => {
              setConversationId(null);
              setResult(null);
              setError(null);
            }}
            className="rounded-xl border border-border px-4 py-2 text-sm text-slate-100"
          >
            New conversation
          </button>
        </div>
        {conversationId && (
          <p className="font-mono text-xs text-muted">conversationId={conversationId}</p>
        )}
      </section>

      {error && (
        <div className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          {error}
        </div>
      )}

      {result && (
        <>
          <section className="grid gap-3 rounded-2xl border border-border bg-panel/40 p-5 sm:grid-cols-3">
            <div>
              <p className="text-xs uppercase tracking-wide text-muted">Status</p>
              <p className="mt-1 font-mono text-sm text-slate-100">{result.status}</p>
            </div>
            <div>
              <p className="text-xs uppercase tracking-wide text-muted">Iterations</p>
              <p className="mt-1 font-mono text-sm text-slate-100">{result.iterations}</p>
            </div>
            <div>
              <p className="text-xs uppercase tracking-wide text-muted">Node</p>
              <p className="mt-1 font-mono text-sm text-slate-100">{result.currentNode}</p>
            </div>
          </section>

          <section className="space-y-2 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Final answer</h2>
            <p className="whitespace-pre-wrap text-sm text-slate-100">{result.response}</p>
          </section>

          <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Graph steps (working state only)</h2>
            <ol className="relative space-y-0 border-l border-border pl-5">
              {result.steps.map((step, index) => (
                <li key={`${step.node}-${index}`} className="relative pb-4 last:pb-0">
                  <span className="absolute -left-[1.4rem] top-1.5 h-2.5 w-2.5 rounded-full bg-accent" />
                  <p className="font-mono text-xs text-accent">
                    {index + 1}. {step.node}
                    {step.action ? ` · ${step.action}` : ''}
                  </p>
                </li>
              ))}
            </ol>
          </section>
        </>
      )}
    </div>
  );
}
