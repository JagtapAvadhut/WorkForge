import { useQuery } from '@tanstack/react-query';
import { memoryApi, type Conversation } from '@/api/memoryApi';

interface Props {
  conversationId: string | null;
  onChange: (id: string | null) => void;
  onCreated?: (conversation: Conversation) => void;
}

export function ConversationPicker({ conversationId, onChange, onCreated }: Props) {
  const query = useQuery({
    queryKey: ['conversations'],
    queryFn: memoryApi.listConversations,
  });

  return (
    <div className="flex flex-wrap items-center gap-2">
      <label className="text-xs uppercase tracking-wide text-muted">Conversation</label>
      <select
        value={conversationId ?? ''}
        onChange={(e) => onChange(e.target.value || null)}
        className="rounded-xl border border-border bg-canvas px-3 py-2 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
      >
        <option value="">New (auto on send)</option>
        {(query.data ?? []).map((c) => (
          <option key={c.id} value={c.id}>
            {c.title || c.id.slice(0, 8)}
          </option>
        ))}
      </select>
      <button
        type="button"
        className="rounded-lg border border-border px-3 py-1.5 text-xs text-slate-200"
        onClick={async () => {
          const created = await memoryApi.createConversation();
          onChange(created.id);
          onCreated?.(created);
          await query.refetch();
        }}
      >
        New
      </button>
    </div>
  );
}
