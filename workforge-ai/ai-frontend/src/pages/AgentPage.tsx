import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { agentApi, type AgentResult } from '@/api/agentApi';
import { formatApiError } from '@/api/client';
import { AppNav } from '@/components/AppNav';
import { ConversationPicker } from '@/components/ConversationPicker';
import { SampleChips } from '@/components/SampleChips';

const AGENT_SAMPLES = [
  'What is a sprint?',
  'Get details of MWS-1',
  'Tell me about project MWS and its open issues',
  'Investigate MWS-1',
];

function errorMessage(err: unknown): string {
  return formatApiError(err);
}

export default function AgentPage() {
  const [message, setMessage] = useState('Tell me about project MWS and its open issues');
  const [maxSteps, setMaxSteps] = useState(5);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [result, setResult] = useState<AgentResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => agentApi.run(message.trim(), maxSteps, conversationId),
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
        <h1 className="text-2xl font-semibold text-slate-50">AI Agent</h1>
        <p className="mt-1 text-sm text-muted">
          Phase 7 + 10 — decide → tool → observe loop with conversation memory. Agent steps stay in-request
          only.
        </p>
      </header>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <ConversationPicker conversationId={conversationId} onChange={setConversationId} />
        <SampleChips samples={AGENT_SAMPLES} disabled={mutation.isPending} onSelect={setMessage} />
        <label className="block text-xs uppercase tracking-wide text-muted">Request</label>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          rows={3}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <div className="flex flex-wrap items-center gap-3">
          <label className="text-xs uppercase tracking-wide text-muted" htmlFor="agent-max-steps">
            Max steps
          </label>
          <input
            id="agent-max-steps"
            type="number"
            min={1}
            max={10}
            value={maxSteps}
            onChange={(e) => setMaxSteps(Number(e.target.value))}
            className="w-24 rounded-xl border border-border bg-canvas px-3 py-2 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
          />
          <button
            type="button"
            disabled={mutation.isPending || !message.trim()}
            onClick={() => mutation.mutate()}
            className="rounded-xl bg-accent px-5 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
          >
            {mutation.isPending ? 'Running agent…' : 'Run agent'}
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
          <section className="space-y-2 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Final answer</h2>
            <p className="whitespace-pre-wrap text-sm text-slate-100">{result.answer}</p>
            <p className="font-mono text-xs text-muted">
              stop={result.stopReason} · iterations={result.iterations}
            </p>
          </section>

          <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Steps (not persisted as memory)</h2>
            <ol className="space-y-3">
              {result.steps.map((step) => (
                <li key={step.stepNumber} className="rounded-xl border border-border bg-canvas/60 px-4 py-3">
                  <p className="font-mono text-xs text-accent">
                    #{step.stepNumber} · {step.action} · {step.status}
                  </p>
                  {step.thought && <p className="mt-1 text-sm text-slate-300">{step.thought}</p>}
                  {step.tool && (
                    <p className="mt-1 font-mono text-xs text-muted">
                      tool={step.tool} args={JSON.stringify(step.arguments)}
                    </p>
                  )}
                  {step.observation && (
                    <pre className="mt-2 max-h-40 overflow-auto whitespace-pre-wrap font-mono text-[11px] text-muted">
                      {step.observation}
                    </pre>
                  )}
                </li>
              ))}
            </ol>
          </section>
        </>
      )}
    </div>
  );
}
