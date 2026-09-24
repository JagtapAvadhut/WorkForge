import { useMutation } from '@tanstack/react-query';
import { useRef, useState } from 'react';
import { aiApi } from '@/api/aiApi';
import { formatApiError } from '@/api/client';
import { AppNav } from '@/components/AppNav';
import { ChatComposer } from '@/components/ChatComposer';
import { ConversationPicker } from '@/components/ConversationPicker';
import { MessageBubble } from '@/components/MessageBubble';
import { SampleChips } from '@/components/SampleChips';
import { StrategySelector } from '@/components/StrategySelector';
import type { ChatMessage, PromptStrategy } from '@/types/chat';

const CHAT_SAMPLES = [
  'Explain what a sprint is.',
  'Explain RAG in simple language.',
  'Explain Agent vs Tool Calling.',
  'Explain MCP using WorkForge AI.',
  'Explain embeddings with an example.',
];

function nextId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export default function AiChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [strategy, setStrategy] = useState<PromptStrategy>('GENERAL');
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const bottomRef = useRef<HTMLDivElement | null>(null);

  function scrollToBottom() {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }

  const mutation = useMutation({
    mutationFn: (payload: { message: string; strategy: PromptStrategy; conversationId: string | null }) =>
      aiApi.chat({
        message: payload.message,
        strategy: payload.strategy,
        conversationId: payload.conversationId,
      }),
    onSuccess: (data) => {
      if (data.conversationId) {
        setConversationId(data.conversationId);
      }
      setMessages((prev) => [
        ...prev,
        {
          id: nextId(),
          role: 'assistant',
          content: data.response,
          strategy: data.strategy,
          createdAt: new Date().toISOString(),
        },
      ]);
      setError(null);
      scrollToBottom();
    },
    onError: (err: unknown) => {
      setError(formatApiError(err));
    },
  });

  function handleSend(text: string) {
    setError(null);
    setMessages((prev) => [
      ...prev,
      { id: nextId(), role: 'user', content: text, createdAt: new Date().toISOString() },
    ]);
    mutation.mutate({ message: text, strategy, conversationId });
    scrollToBottom();
  }

  function handleConversationChange(id: string | null) {
    setConversationId(id);
    setMessages([]);
    setError(null);
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col px-4 py-6">
      <AppNav />
      <header className="mb-4 rounded-2xl border border-border bg-panel/70 px-5 py-4 backdrop-blur">
        <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">WorkForge AI</p>
        <h1 className="mt-1 text-2xl font-semibold text-slate-50">Prompt engineering chat</h1>
        <p className="mt-1 text-sm text-muted">
          Phase 2 + 10 — strategies via Ollama, with server-side conversation memory.
        </p>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <StrategySelector value={strategy} onChange={setStrategy} disabled={mutation.isPending} />
          <ConversationPicker conversationId={conversationId} onChange={handleConversationChange} />
        </div>
        <div className="mt-4">
          <SampleChips
            samples={CHAT_SAMPLES}
            disabled={mutation.isPending}
            onSelect={setDraft}
          />
        </div>
      </header>

      <section className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-2xl border border-border bg-panel/40">
        <div className="flex-1 space-y-4 overflow-y-auto p-4 md:p-6">
          {messages.length === 0 && !mutation.isPending && (
            <div className="flex h-full min-h-[280px] flex-col items-center justify-center text-center">
              <p className="text-lg font-medium text-slate-100">Ask anything about WorkForge</p>
              <p className="mt-2 max-w-md text-sm text-muted">
                Continue a conversation to demonstrate short-term memory, or open /memory for long-term
                preferences.
              </p>
            </div>
          )}

          {messages.map((m) => (
            <MessageBubble key={m.id} message={m} />
          ))}

          {mutation.isPending && (
            <div className="flex justify-start">
              <div className="rounded-2xl bg-panel px-4 py-3 text-sm text-muted ring-1 ring-border">
                Generating with {strategy}…
              </div>
            </div>
          )}

          {error && (
            <div className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">
              {error}
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        <ChatComposer
          disabled={mutation.isPending}
          draft={draft}
          onDraftChange={setDraft}
          onSend={handleSend}
        />
      </section>
    </div>
  );
}
