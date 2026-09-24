import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { multiAgentApi } from '@/api/multiAgentApi';
import { observabilityApi, type TraceDto } from '@/api/observabilityApi';
import { formatApiError } from '@/api/client';
import { ApiErrorBanner } from '@/components/ApiErrorBanner';
import { AppNav } from '@/components/AppNav';

export default function ObservabilityPage() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [offset, setOffset] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [demoNote, setDemoNote] = useState<string | null>(null);
  const limit = 20;

  const summaryQuery = useQuery({
    queryKey: ['obs-summary'],
    queryFn: observabilityApi.summary,
    refetchInterval: 15_000,
  });

  const tracesQuery = useQuery({
    queryKey: ['obs-traces', offset],
    queryFn: () => observabilityApi.traces(limit, offset),
    refetchInterval: 15_000,
  });

  const detailQuery = useQuery({
    queryKey: ['obs-trace', selectedId],
    queryFn: () => observabilityApi.trace(selectedId!),
    enabled: !!selectedId,
  });

  const demoMutation = useMutation({
    mutationFn: () => multiAgentApi.run('Investigate MWS-1'),
    onSuccess: async () => {
      setError(null);
      setDemoNote('Demo request completed. Refreshing traces…');
      await queryClient.invalidateQueries({ queryKey: ['obs-summary'] });
      await queryClient.invalidateQueries({ queryKey: ['obs-traces'] });
      const page = await observabilityApi.traces(limit, 0);
      setOffset(0);
      if (page.items[0]) setSelectedId(page.items[0].traceId);
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const summary = summaryQuery.data;
  const page = tracesQuery.data;
  const detail: TraceDto | undefined = detailQuery.data;

  function refresh() {
    void summaryQuery.refetch();
    void tracesQuery.refetch();
    if (selectedId) void detailQuery.refetch();
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-5xl flex-col gap-6 px-4 py-6">
      <AppNav />
      <header className="rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-slate-50">Observability</h1>
            <p className="mt-1 text-sm text-muted">
              Phase 12 — execution summaries only. No secrets, no hidden chain-of-thought. Token fields
              appear only when the model provider exposes usage.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="rounded-xl border border-border px-4 py-2 text-sm text-slate-100"
              onClick={refresh}
            >
              Refresh
            </button>
            <button
              type="button"
              disabled={demoMutation.isPending}
              onClick={() => demoMutation.mutate()}
              className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
            >
              {demoMutation.isPending ? 'Running…' : 'Run demo request'}
            </button>
          </div>
        </div>
        {demoNote && <p className="mt-3 text-xs text-muted">{demoNote}</p>}
      </header>

      <ApiErrorBanner
        error={error ?? (summaryQuery.isError || tracesQuery.isError ? summaryQuery.error ?? tracesQuery.error : null)}
        onRetry={refresh}
      />

      {summaryQuery.isLoading && <p className="text-sm text-muted">Loading summary…</p>}

      {summary && (
        <section className="grid gap-3 rounded-2xl border border-border bg-panel/40 p-5 sm:grid-cols-4">
          <Metric label="Total" value={String(summary.totalRequests)} />
          <Metric label="Success" value={String(summary.successfulRequests)} />
          <Metric label="Failure" value={String(summary.failedRequests)} />
          <Metric label="Avg Latency" value={`${Math.round(summary.averageLatencyMs)} ms`} />
          <Metric label="Tool Calls" value={String(summary.toolCallCount)} />
          <Metric label="Agent Executions" value={String(summary.agentExecutions)} />
          <Metric label="MCP Calls" value={String(summary.mcpExecutions)} />
          <Metric label="RAG Retrievals" value={String(summary.ragRetrievalCount)} />
        </section>
      )}

      <section className="rounded-2xl border border-border bg-panel/40 p-5">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-lg font-medium text-slate-100">Recent traces</h2>
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded-lg border border-border px-3 py-1 text-xs text-slate-200 disabled:opacity-40"
              disabled={offset <= 0}
              onClick={() => setOffset((v) => Math.max(0, v - limit))}
            >
              Prev
            </button>
            <button
              type="button"
              className="rounded-lg border border-border px-3 py-1 text-xs text-slate-200 disabled:opacity-40"
              disabled={!page || offset + limit >= page.total}
              onClick={() => setOffset((v) => v + limit)}
            >
              Next
            </button>
          </div>
        </div>
        {(page?.items ?? []).length === 0 && !tracesQuery.isLoading && (
          <p className="text-sm text-muted">No traces yet. Click “Run demo request”.</p>
        )}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-xs uppercase tracking-wide text-muted">
              <tr>
                <th className="pb-2 pr-3">Trace ID</th>
                <th className="pb-2 pr-3">Feature</th>
                <th className="pb-2 pr-3">Model</th>
                <th className="pb-2 pr-3">Latency</th>
                <th className="pb-2 pr-3">Success</th>
                <th className="pb-2">Time</th>
              </tr>
            </thead>
            <tbody>
              {(page?.items ?? []).map((row) => (
                <tr
                  key={row.traceId}
                  className="cursor-pointer border-t border-border/60 hover:bg-canvas/40"
                  onClick={() => setSelectedId(row.traceId)}
                >
                  <td className="py-2 pr-3 font-mono text-xs text-accent">{row.traceId.slice(0, 12)}…</td>
                  <td className="py-2 pr-3">{row.feature}</td>
                  <td className="py-2 pr-3 font-mono text-xs text-muted">{row.model}</td>
                  <td className="py-2 pr-3 font-mono text-muted">{row.latencyMs ?? '—'} ms</td>
                  <td className="py-2 pr-3">{row.success ? 'yes' : 'no'}</td>
                  <td className="py-2 font-mono text-xs text-muted">
                    {row.startedAt ? new Date(row.startedAt).toLocaleString() : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {detail && (
        <section className="space-y-2 rounded-2xl border border-border bg-panel/40 p-5">
          <h2 className="text-lg font-medium text-slate-100">Trace detail</h2>
          <p className="font-mono text-xs text-accent">{detail.traceId}</p>
          <pre className="overflow-x-auto whitespace-pre-wrap font-mono text-[11px] text-muted">
            {JSON.stringify(
              {
                feature: detail.feature,
                model: detail.model,
                latencyMs: detail.latencyMs,
                success: detail.success,
                errorCode: detail.errorCode,
                requestSummary: detail.requestSummary,
                toolCallCount: detail.toolCallCount,
                agentStepCount: detail.agentStepCount,
                mcpCallCount: detail.mcpCallCount,
                retrievalCount: detail.retrievalCount,
                ...(detail.inputTokens != null ? { inputTokens: detail.inputTokens } : {}),
                ...(detail.outputTokens != null ? { outputTokens: detail.outputTokens } : {}),
                ...(detail.totalTokens != null ? { totalTokens: detail.totalTokens } : {}),
              },
              null,
              2,
            )}
          </pre>
        </section>
      )}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-muted">{label}</p>
      <p className="mt-1 font-mono text-sm text-slate-100">{value}</p>
    </div>
  );
}
