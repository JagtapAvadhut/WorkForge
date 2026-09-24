import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { toolsApi, type ToolChatResult, type ToolPreviewResult } from '@/api/toolsApi';
import { formatApiError } from '@/api/client';
import { AppNav } from '@/components/AppNav';
import { SampleChips } from '@/components/SampleChips';

const TOOL_SAMPLES = [
  'Get MWS-1',
  'Show open MWS issues',
  'Tell me about project MWS',
  'Show open MWS issues assigned to Avadhoot',
];

export default function ToolsPage() {
  const [message, setMessage] = useState('Show open MWS issues assigned to Avadhoot');
  const [result, setResult] = useState<ToolChatResult | null>(null);
  const [preview, setPreview] = useState<ToolPreviewResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const chatMutation = useMutation({
    mutationFn: () => toolsApi.chat(message.trim()),
    onSuccess: (data) => {
      setResult(data);
      setError(null);
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const previewMutation = useMutation({
    mutationFn: () => toolsApi.preview(message.trim()),
    onSuccess: (data) => {
      setPreview(data);
      setError(null);
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const busy = chatMutation.isPending || previewMutation.isPending;

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col gap-6 px-4 py-6">
      <AppNav />

      <header className="rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <h1 className="text-2xl font-semibold text-slate-50">Tool chat</h1>
        <p className="mt-1 text-sm text-muted">
          Phase 6 — Spring AI tool calling. The model may call getIssue, searchIssues, or getProject;
          Java executes read-only queries.
        </p>
      </header>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <SampleChips samples={TOOL_SAMPLES} disabled={busy} onSelect={setMessage} />
        <label className="block text-xs uppercase tracking-wide text-muted">Message</label>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          rows={3}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <div className="flex flex-wrap gap-3">
          <button
            type="button"
            disabled={busy || !message.trim()}
            onClick={() => chatMutation.mutate()}
            className="rounded-xl bg-accent px-5 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
          >
            {chatMutation.isPending ? 'Calling tools…' : 'Send'}
          </button>
          <button
            type="button"
            disabled={busy || !message.trim()}
            onClick={() => previewMutation.mutate()}
            className="rounded-xl border border-border px-5 py-2 text-sm text-slate-100 disabled:opacity-50"
          >
            Preview tool defs
          </button>
        </div>
      </section>

      {error && (
        <div className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          {error}
        </div>
      )}

      {result && (
        <>
          <section className="space-y-2 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Response</h2>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-100">{result.response}</p>
          </section>

          <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="text-lg font-medium text-slate-100">Tool Calls</h2>
            {result.toolCalls.length === 0 ? (
              <p className="text-sm text-muted">No tools were invoked for this message.</p>
            ) : (
              <ul className="space-y-3">
                {result.toolCalls.map((call, index) => (
                  <li key={`${call.tool}-${index}`} className="rounded-xl border border-border bg-canvas/60 px-4 py-3 text-sm">
                    <p className="font-mono text-xs text-accent">
                      #{index + 1} · {call.tool}
                    </p>
                    <p className="mt-2 font-mono text-[11px] text-muted">
                      arguments: {JSON.stringify(call.arguments)}
                    </p>
                    <p className="mt-2 whitespace-pre-wrap font-mono text-[11px] text-slate-300">
                      result: {call.resultSummary}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}

      {preview && (
        <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
          <h2 className="text-lg font-medium text-slate-100">Tool definitions (preview)</h2>
          <ul className="space-y-3">
            {preview.tools.map((tool) => (
              <li key={tool.name} className="rounded-xl border border-border bg-canvas/60 px-4 py-3 text-sm">
                <p className="font-mono text-xs text-accent">{tool.name}</p>
                <p className="mt-1 text-slate-200">{tool.description}</p>
                <pre className="mt-2 overflow-x-auto rounded-lg bg-black/30 p-2 font-mono text-[10px] text-muted">
                  {tool.inputSchema}
                </pre>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
