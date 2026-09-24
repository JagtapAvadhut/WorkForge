import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { multiAgentApi, type MultiAgentResult } from '@/api/multiAgentApi';
import { formatApiError } from '@/api/client';
import { AppNav } from '@/components/AppNav';
import { ConversationPicker } from '@/components/ConversationPicker';
import { SampleChips } from '@/components/SampleChips';

const MULTI_SAMPLES = [
  'Investigate MWS-1 and explain the issue, project context and relevant documentation.',
  'Give me project and issue information for MWS.',
  'Find MWS-1 and relevant documentation.',
];

function errorMessage(err: unknown): string {
  return formatApiError(err);
}

export default function MultiAgentPage() {
  const [message, setMessage] = useState(
    'Investigate MWS-1 and explain the issue, project context and relevant documentation.',
  );
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [result, setResult] = useState<MultiAgentResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => multiAgentApi.run(message.trim(), conversationId),
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
        <h1 className="text-2xl font-semibold text-slate-50">Multi-Agent</h1>
        <p className="mt-1 text-sm text-muted">
          Phase 11 — Supervisor coordinates Issue, Knowledge (RAG), and Project specialists on the same
          WorkForge dataset.
        </p>
      </header>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <ConversationPicker conversationId={conversationId} onChange={setConversationId} />
        <SampleChips samples={MULTI_SAMPLES} disabled={mutation.isPending} onSelect={setMessage} />
        <label className="block text-xs uppercase tracking-wide text-muted">Request</label>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          rows={3}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <button
          type="button"
          disabled={mutation.isPending || !message.trim()}
          onClick={() => mutation.mutate()}
          className="rounded-xl bg-accent px-5 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
        >
          {mutation.isPending ? 'Running multi-agent…' : 'Run multi-agent'}
        </button>
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
              <p className="text-xs uppercase tracking-wide text-muted">Specialist calls</p>
              <p className="mt-1 font-mono text-sm text-slate-100">{result.specialistCalls}</p>
            </div>
          </section>

          <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Timeline</h2>
            <ol className="relative space-y-0 border-l border-border pl-5">
              {result.steps.map((step, index) => (
                <li key={`${step.node}-${index}`} className="relative pb-4 last:pb-0">
                  <span className="absolute -left-[1.4rem] top-1.5 h-2.5 w-2.5 rounded-full bg-accent" />
                  <p className="font-mono text-xs text-accent">
                    {index + 1}. {step.node}
                    {step.action ? ` · ${step.action}` : ''}
                    {step.node.endsWith('_AGENT') ? ' ✅' : ''}
                  </p>
                  {step.details && Object.keys(step.details).length > 0 && (
                    <pre className="mt-1 overflow-x-auto font-mono text-[11px] text-muted">
                      {JSON.stringify(step.details, null, 2)}
                    </pre>
                  )}
                </li>
              ))}
            </ol>
            {result.agentsUsed.length > 0 && (
              <p className="font-mono text-xs text-muted">
                agentsUsed={result.agentsUsed.join(', ')}
              </p>
            )}
          </section>

          {result.specialistResults.length > 0 && (
            <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
              <h2 className="text-lg font-medium text-slate-100">Findings</h2>
              {result.specialistResults.map((row, index) => (
                <div key={index} className="rounded-xl border border-border bg-canvas/60 px-4 py-3">
                  <p className="font-mono text-xs text-accent">
                    {String(row.agent)} · {String(row.status)}
                  </p>
                  <p className="mt-1 text-sm text-slate-200">{String(row.summary ?? '')}</p>
                </div>
              ))}
            </section>
          )}

          {result.sources.length > 0 && (
            <section className="space-y-2 rounded-2xl border border-border bg-panel/40 p-5">
              <h2 className="text-lg font-medium text-slate-100">Sources</h2>
              <pre className="overflow-x-auto font-mono text-[11px] text-muted">
                {JSON.stringify(result.sources, null, 2)}
              </pre>
            </section>
          )}

          <section className="space-y-2 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Final answer</h2>
            <p className="whitespace-pre-wrap text-sm text-slate-100">{result.answer}</p>
          </section>
        </>
      )}
    </div>
  );
}
