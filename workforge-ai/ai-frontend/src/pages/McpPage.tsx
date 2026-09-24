import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { mcpApi, type McpAgentResult, type McpChatResult } from '@/api/mcpApi';
import { formatApiError } from '@/api/client';
import { AppNav } from '@/components/AppNav';
import { ConversationPicker } from '@/components/ConversationPicker';
import { SampleChips } from '@/components/SampleChips';

const MCP_SAMPLES = [
  'Get MWS-1',
  'Show open MWS issues',
  'Tell me about project MWS',
  'Tell me about MWS-1 and its project.',
];

function errorMessage(err: unknown): string {
  return formatApiError(err);
}

export default function McpPage() {
  const [chatMessage, setChatMessage] = useState('Show me MWS-1');
  const [agentMessage, setAgentMessage] = useState('Tell me about MWS-1 and its project.');
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [chatResult, setChatResult] = useState<McpChatResult | null>(null);
  const [agentResult, setAgentResult] = useState<McpAgentResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const statusQuery = useQuery({
    queryKey: ['mcp-status'],
    queryFn: mcpApi.status,
    refetchInterval: 10_000,
    retry: false,
  });

  const toolsQuery = useQuery({
    queryKey: ['mcp-tools'],
    queryFn: mcpApi.tools,
    retry: false,
  });

  const chatMutation = useMutation({
    mutationFn: () => mcpApi.chat(chatMessage.trim()),
    onSuccess: (data) => {
      setChatResult(data);
      setError(null);
    },
    onError: (err) => setError(errorMessage(err)),
  });

  const agentMutation = useMutation({
    mutationFn: () => mcpApi.agent(agentMessage.trim(), 5, conversationId),
    onSuccess: (data) => {
      setAgentResult(data);
      if (data.conversationId) setConversationId(data.conversationId);
      setError(null);
    },
    onError: (err) => setError(errorMessage(err)),
  });

  const status = statusQuery.data;

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col gap-6 px-4 py-6">
      <AppNav />

      <header className="rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <h1 className="text-2xl font-semibold text-slate-50">MCP</h1>
        <p className="mt-1 text-sm text-muted">
          Phase 9 + 10 — MCP tools (:8091) with conversation memory on the AI backend. Protocol unchanged.
        </p>
      </header>

      <section className="grid gap-3 rounded-2xl border border-border bg-panel/40 p-5 sm:grid-cols-2">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted">MCP Server</p>
          <p className="mt-1 font-mono text-sm text-slate-100">
            {status ? (status.serverUp ? 'UP' : 'DOWN') : statusQuery.isError ? 'DOWN' : '…'}
          </p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted">MCP Client</p>
          <p className="mt-1 font-mono text-sm text-slate-100">
            {status ? (status.clientConnected ? 'CONNECTED' : 'DISCONNECTED') : '…'}
          </p>
        </div>
        <div className="sm:col-span-2">
          <p className="text-xs uppercase tracking-wide text-muted">Endpoint</p>
          <p className="mt-1 font-mono text-xs text-accent">
            {status?.endpoint ?? 'http://localhost:8091/mcp'}
          </p>
          {status?.detail && <p className="mt-1 text-xs text-muted">{status.detail}</p>}
        </div>
      </section>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-lg font-medium text-slate-100">Discovered tools</h2>
          <button
            type="button"
            className="rounded-lg border border-border px-3 py-1 text-xs text-slate-200"
            onClick={() => {
              void statusQuery.refetch();
              void toolsQuery.refetch();
            }}
          >
            Refresh tools
          </button>
        </div>
        {toolsQuery.isError && (
          <p className="text-sm text-red-200">{errorMessage(toolsQuery.error)}</p>
        )}
        {toolsQuery.data?.length === 0 && (
          <p className="text-sm text-muted">No tools discovered (is MCP server running?).</p>
        )}
        <ul className="space-y-2">
          {(toolsQuery.data ?? []).map((tool) => (
            <li key={tool.name} className="rounded-xl border border-border bg-canvas/60 px-4 py-3">
              <p className="font-mono text-sm text-accent">{tool.name}</p>
              <p className="mt-1 text-sm text-slate-300">{tool.description}</p>
              <pre className="mt-2 overflow-x-auto font-mono text-[11px] text-muted">
                {JSON.stringify(tool.parameters, null, 2)}
              </pre>
            </li>
          ))}
        </ul>
      </section>

      {error && (
        <div className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          {error}
        </div>
      )}

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <h2 className="text-lg font-medium text-slate-100">MCP tool chat</h2>
        <SampleChips samples={MCP_SAMPLES} disabled={chatMutation.isPending} onSelect={setChatMessage} />
        <textarea
          value={chatMessage}
          onChange={(e) => setChatMessage(e.target.value)}
          rows={2}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <button
          type="button"
          disabled={chatMutation.isPending || !chatMessage.trim()}
          onClick={() => chatMutation.mutate()}
          className="rounded-xl bg-accent px-5 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
        >
          {chatMutation.isPending ? 'Running…' : 'Run MCP chat'}
        </button>
        {chatResult && (
          <div className="space-y-2 rounded-xl border border-border bg-canvas/60 px-4 py-3 text-sm">
            <p className="whitespace-pre-wrap text-slate-100">{chatResult.response}</p>
            <p className="font-mono text-xs text-muted">
              toolsUsed={chatResult.toolsUsed.length
                ? chatResult.toolsUsed.map((t) => String(t.name)).join(', ')
                : 'none'}
            </p>
          </div>
        )}
      </section>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <h2 className="text-lg font-medium text-slate-100">MCP agent (LangGraph + MCP)</h2>
        <ConversationPicker conversationId={conversationId} onChange={setConversationId} />
        <SampleChips samples={MCP_SAMPLES} disabled={agentMutation.isPending} onSelect={setAgentMessage} />
        <textarea
          value={agentMessage}
          onChange={(e) => setAgentMessage(e.target.value)}
          rows={2}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
        />
        <button
          type="button"
          disabled={agentMutation.isPending || !agentMessage.trim()}
          onClick={() => agentMutation.mutate()}
          className="rounded-xl bg-accent px-5 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
        >
          {agentMutation.isPending ? 'Running…' : 'Run MCP agent'}
        </button>
        {conversationId && (
          <p className="font-mono text-xs text-muted">conversationId={conversationId}</p>
        )}
        {agentResult && (
          <div className="space-y-3">
            <p className="whitespace-pre-wrap text-sm text-slate-100">{agentResult.response}</p>
            <p className="font-mono text-xs text-muted">
              status={agentResult.status} · iterations={agentResult.iterations} · node=
              {agentResult.currentNode}
            </p>
            <ol className="relative space-y-0 border-l border-border pl-5">
              {agentResult.steps.map((step, index) => (
                <li key={`${step.node}-${index}`} className="relative pb-4 last:pb-0">
                  <span className="absolute -left-[1.4rem] top-1.5 h-2.5 w-2.5 rounded-full bg-accent" />
                  <p className="font-mono text-xs text-accent">
                    {index + 1}. {step.node}
                    {step.action ? ` · ${step.action}` : ''}
                  </p>
                </li>
              ))}
            </ol>
          </div>
        )}
      </section>
    </div>
  );
}
