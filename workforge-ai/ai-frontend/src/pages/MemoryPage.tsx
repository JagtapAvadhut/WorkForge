import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { demoApi } from '@/api/demoApi';
import { memoryApi, getSessionId } from '@/api/memoryApi';
import { formatApiError } from '@/api/client';
import { ApiErrorBanner } from '@/components/ApiErrorBanner';
import { AppNav } from '@/components/AppNav';
import { SampleChips } from '@/components/SampleChips';

const MEMORY_DEMO_PROMPTS = [
  'What is RAG?',
  'Explain it using a simple example.',
  'Explain embeddings.',
];

export default function MemoryPage() {
  const queryClient = useQueryClient();
  const [selectedConversation, setSelectedConversation] = useState<string | null>(null);
  const [category, setCategory] = useState('preference');
  const [content, setContent] = useState('User prefers simple explanations.');
  const [importance, setImportance] = useState(5);
  const [error, setError] = useState<string | null>(null);
  const [seedNote, setSeedNote] = useState<string | null>(null);
  const [demoPrompt, setDemoPrompt] = useState(MEMORY_DEMO_PROMPTS[0]);

  const conversationsQuery = useQuery({
    queryKey: ['conversations'],
    queryFn: memoryApi.listConversations,
  });

  const messagesQuery = useQuery({
    queryKey: ['conversation-messages', selectedConversation],
    queryFn: () => memoryApi.getMessages(selectedConversation!),
    enabled: !!selectedConversation,
  });

  const memoriesQuery = useQuery({
    queryKey: ['long-term-memories'],
    queryFn: memoryApi.listMemories,
  });

  const createConversation = useMutation({
    mutationFn: () => memoryApi.createConversation('Demo conversation'),
    onSuccess: async (created) => {
      setSelectedConversation(created.id);
      await queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const deleteConversation = useMutation({
    mutationFn: (id: string) => memoryApi.deleteConversation(id),
    onSuccess: async () => {
      setSelectedConversation(null);
      await queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const remember = useMutation({
    mutationFn: () => memoryApi.remember({ category, content, importance }),
    onSuccess: async () => {
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ['long-term-memories'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const deleteMemory = useMutation({
    mutationFn: (id: string) => memoryApi.deleteMemory(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['long-term-memories'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const clearMemories = useMutation({
    mutationFn: async () => {
      const items = await memoryApi.listMemories();
      for (const m of items) {
        await memoryApi.deleteMemory(m.id);
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['long-term-memories'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  const loadDemo = useMutation({
    mutationFn: async () => {
      const seed = await demoApi.seed();
      if (!seed.memoryCreated) {
        await memoryApi.remember({
          category: 'preference',
          content: 'User prefers simple explanations.',
          importance: 5,
        });
      }
      const created = await memoryApi.createConversation('Memory demo');
      return { seed, conversationId: created.id };
    },
    onSuccess: async (result) => {
      setSelectedConversation(result.conversationId);
      setCategory('preference');
      setContent('User prefers simple explanations.');
      setImportance(5);
      setSeedNote(
        `Demo ready (session ${getSessionId()}). Created docs=${result.seed.documentsCreated}, ` +
          `memoryCreated=${result.seed.memoryCreated || true}. Use Chat/Agent with this conversation ` +
          `to ask: "${MEMORY_DEMO_PROMPTS[0]}" then "${MEMORY_DEMO_PROMPTS[1]}".`,
      );
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ['conversations'] });
      await queryClient.invalidateQueries({ queryKey: ['long-term-memories'] });
    },
    onError: (err) => setError(formatApiError(err)),
  });

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col gap-6 px-4 py-6">
      <AppNav />

      <header className="rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-slate-50">Memory</h1>
            <p className="mt-1 text-sm text-muted">
              Phase 10 — conversation history (short-term) and explicit long-term notes. The app retrieves
              memory and sends it as context; the model does not remember by itself.
            </p>
          </div>
          <button
            type="button"
            disabled={loadDemo.isPending}
            onClick={() => loadDemo.mutate()}
            className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
          >
            {loadDemo.isPending ? 'Loading…' : 'Load demo memory'}
          </button>
        </div>
        {seedNote && <p className="mt-3 text-xs text-muted">{seedNote}</p>}
      </header>

      <ApiErrorBanner error={error} onRetry={() => setError(null)} />

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <p className="text-sm text-slate-300">
          Conversation demo prompts (copy into Chat / Agent with the selected conversation):
        </p>
        <SampleChips samples={MEMORY_DEMO_PROMPTS} onSelect={setDemoPrompt} />
        <p className="font-mono text-xs text-accent">{demoPrompt}</p>
      </section>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-medium text-slate-100">Conversations</h2>
          <button
            type="button"
            onClick={() => createConversation.mutate()}
            className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950"
          >
            New conversation
          </button>
        </div>
        {conversationsQuery.isLoading && <p className="text-sm text-muted">Loading…</p>}
        {(conversationsQuery.data ?? []).length === 0 && !conversationsQuery.isLoading && (
          <p className="text-sm text-muted">No conversations yet.</p>
        )}
        <ul className="space-y-2">
          {(conversationsQuery.data ?? []).map((c) => (
            <li
              key={c.id}
              className={`flex items-center justify-between gap-3 rounded-xl border px-4 py-3 text-sm ${
                selectedConversation === c.id ? 'border-accent bg-accent/10' : 'border-border bg-canvas/60'
              }`}
            >
              <button type="button" className="text-left text-slate-100" onClick={() => setSelectedConversation(c.id)}>
                <p className="font-medium">{c.title || 'Untitled'}</p>
                <p className="font-mono text-[11px] text-muted">{c.id}</p>
              </button>
              <button
                type="button"
                className="text-xs text-red-300"
                onClick={() => deleteConversation.mutate(c.id)}
              >
                Delete
              </button>
            </li>
          ))}
        </ul>
        {selectedConversation && (
          <div className="space-y-2 border-t border-border pt-4">
            <h3 className="text-sm font-medium text-slate-200">Messages</h3>
            {(messagesQuery.data ?? []).length === 0 && (
              <p className="text-sm text-muted">No messages yet — send from Chat / Agent pages.</p>
            )}
            {(messagesQuery.data ?? []).map((m) => (
              <div key={m.id} className="rounded-lg border border-border bg-canvas/40 px-3 py-2 text-sm">
                <p className="font-mono text-[11px] text-accent">{m.role}</p>
                <p className="mt-1 whitespace-pre-wrap text-slate-200">{m.content}</p>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-medium text-slate-100">Long-term memory</h2>
          <button
            type="button"
            className="rounded-lg border border-border px-3 py-1 text-xs text-slate-200"
            onClick={() => clearMemories.mutate()}
            disabled={clearMemories.isPending}
          >
            Clear memory
          </button>
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
          <input
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="category"
            className="rounded-xl border border-border bg-canvas px-3 py-2 text-sm text-slate-100"
          />
          <input
            type="number"
            min={1}
            max={10}
            value={importance}
            onChange={(e) => setImportance(Number(e.target.value))}
            className="rounded-xl border border-border bg-canvas px-3 py-2 text-sm text-slate-100"
          />
          <button
            type="button"
            onClick={() => remember.mutate()}
            className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950"
          >
            Remember
          </button>
        </div>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={2}
          className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100"
        />
        {(memoriesQuery.data ?? []).length === 0 && (
          <p className="text-sm text-muted">No long-term memories. Use Load demo memory or Remember.</p>
        )}
        <ul className="space-y-2">
          {(memoriesQuery.data ?? []).map((m) => (
            <li key={m.id} className="flex items-start justify-between gap-3 rounded-xl border border-border bg-canvas/60 px-4 py-3">
              <div>
                <p className="font-mono text-xs text-accent">
                  {m.category} · importance={m.importance}
                </p>
                <p className="mt-1 text-sm text-slate-200">{m.content}</p>
              </div>
              <button type="button" className="text-xs text-red-300" onClick={() => deleteMemory.mutate(m.id)}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
