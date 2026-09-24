import { useEffect, useRef, useState } from 'react';
import {
  evaluationApi,
  type EvaluationCaseResult,
  type EvaluationRunSnapshot,
  type EvaluationSuite,
} from '@/api/evaluationApi';
import { formatApiError } from '@/api/client';
import { AppNav } from '@/components/AppNav';

export default function EvaluationPage() {
  const [suite, setSuite] = useState<EvaluationSuite>('SMOKE');
  const [run, setRun] = useState<EvaluationRunSnapshot | null>(null);
  const [selected, setSelected] = useState<EvaluationCaseResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);
  const pollRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (pollRef.current) window.clearInterval(pollRef.current);
    };
  }, []);

  async function start(selectedSuite: EvaluationSuite) {
    setSuite(selectedSuite);
    setStarting(true);
    setError(null);
    setSelected(null);
    if (pollRef.current) {
      window.clearInterval(pollRef.current);
      pollRef.current = null;
    }
    try {
      const started = await evaluationApi.start(selectedSuite);
      setRun(started);
      if (started.status === 'COMPLETED' || started.status === 'FAILED') {
        setStarting(false);
        return;
      }
      pollRef.current = window.setInterval(async () => {
        try {
          const next = await evaluationApi.getRun(started.runId);
          setRun(next);
          if (next.status === 'COMPLETED' || next.status === 'FAILED' || next.status === 'CANCELLED') {
            if (pollRef.current) {
              window.clearInterval(pollRef.current);
              pollRef.current = null;
            }
            setStarting(false);
          }
        } catch (err) {
          setError(formatApiError(err));
          if (pollRef.current) {
            window.clearInterval(pollRef.current);
            pollRef.current = null;
          }
          setStarting(false);
        }
      }, 1000);
    } catch (err) {
      setError(formatApiError(err));
      setStarting(false);
    }
  }

  const running = starting || run?.status === 'RUNNING' || run?.status === 'QUEUED';

  return (
    <div className="mx-auto flex min-h-screen max-w-5xl flex-col gap-6 px-4 py-6">
      <AppNav />
      <header className="rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <h1 className="text-2xl font-semibold text-slate-50">Evaluation</h1>
        <p className="mt-1 text-sm text-muted">
          Async suites with progress polling. SMOKE is fast; ALL runs real Phase 1–11 checks without
          holding one HTTP request open.
        </p>
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <label className="text-xs uppercase tracking-wide text-muted">Suite</label>
          <select
            value={suite}
            disabled={running}
            onChange={(e) => setSuite(e.target.value as EvaluationSuite)}
            className="rounded-xl border border-border bg-canvas px-3 py-2 text-sm text-slate-100"
          >
            <option value="SMOKE">SMOKE</option>
            <option value="CORE">CORE</option>
            <option value="ALL">ALL</option>
          </select>
          <button
            type="button"
            disabled={running}
            onClick={() => void start('SMOKE')}
            className="rounded-xl border border-border px-4 py-2 text-sm text-slate-100 disabled:opacity-50"
          >
            Run Smoke
          </button>
          <button
            type="button"
            disabled={running}
            onClick={() => void start('CORE')}
            className="rounded-xl border border-border px-4 py-2 text-sm text-slate-100 disabled:opacity-50"
          >
            Run Core
          </button>
          <button
            type="button"
            disabled={running}
            onClick={() => void start('ALL')}
            className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
          >
            Run All
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          <p>{error}</p>
          <button
            type="button"
            className="mt-2 rounded-lg border border-border px-3 py-1 text-xs"
            onClick={() => void start(suite)}
          >
            Retry
          </button>
        </div>
      )}

      {run && (
        <>
          <section className="grid gap-3 rounded-2xl border border-border bg-panel/40 p-5 sm:grid-cols-3">
            <Metric label="Status" value={run.status} />
            <Metric
              label="Progress"
              value={`${run.completedCount} / ${run.totalCases}`}
            />
            <Metric
              label="Current"
              value={run.currentCategory || run.currentCaseId || '—'}
            />
            <Metric label="Elapsed" value={`${Math.round(run.elapsedMs / 1000)} sec`} />
            <Metric label="Passed" value={String(run.passedCount)} />
            <Metric label="Failed" value={String(run.failedCount)} />
          </section>

          {(run.status === 'COMPLETED' || run.status === 'FAILED') && (
            <section className="grid gap-3 rounded-2xl border border-border bg-panel/40 p-5 sm:grid-cols-5">
              <Metric label="Total" value={String(run.totalCases)} />
              <Metric label="Passed" value={String(run.passedCount)} />
              <Metric label="Failed" value={String(run.failedCount)} />
              <Metric label="Pass Rate" value={`${run.passRate.toFixed(1)}%`} />
              <Metric label="Avg Latency" value={`${run.averageLatencyMs} ms`} />
            </section>
          )}

          <section className="rounded-2xl border border-border bg-panel/40 p-5">
            <h2 className="mb-3 text-lg font-medium text-slate-100">Test Cases</h2>
            {run.results.length === 0 ? (
              <p className="text-sm text-muted">Waiting for first case…</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-xs uppercase tracking-wide text-muted">
                    <tr>
                      <th className="pb-2 pr-3">ID</th>
                      <th className="pb-2 pr-3">Category</th>
                      <th className="pb-2 pr-3">Result</th>
                      <th className="pb-2">Latency</th>
                    </tr>
                  </thead>
                  <tbody>
                    {run.results.map((row) => (
                      <tr
                        key={row.caseId}
                        className="cursor-pointer border-t border-border/60 hover:bg-canvas/40"
                        onClick={() => setSelected(row)}
                      >
                        <td className="py-2 pr-3 font-mono text-accent">{row.caseId}</td>
                        <td className="py-2 pr-3">{row.category}</td>
                        <td className="py-2 pr-3">{row.passed ? 'PASS' : 'FAIL'}</td>
                        <td className="py-2 font-mono text-muted">{row.latencyMs} ms</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}

      {selected && (
        <section className="space-y-2 rounded-2xl border border-border bg-panel/40 p-5">
          <h2 className="text-lg font-medium text-slate-100">Case detail — {selected.caseId}</h2>
          <pre className="overflow-x-auto whitespace-pre-wrap font-mono text-[11px] text-muted">
            {JSON.stringify(selected, null, 2)}
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
