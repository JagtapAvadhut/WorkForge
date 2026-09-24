import type { ChatMessage } from '@/types/chat';

interface Props {
  message: ChatMessage;
}

export function MessageBubble({ message }: Props) {
  const isUser = message.role === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${
          isUser
            ? 'bg-accent text-slate-950'
            : 'bg-panel text-slate-100 ring-1 ring-border'
        }`}
      >
        {!isUser && message.strategy && (
          <p className="mb-1 font-mono text-[10px] uppercase tracking-wider text-accent/80">
            {message.strategy}
          </p>
        )}
        <p className="whitespace-pre-wrap">{message.content}</p>
      </div>
    </div>
  );
}
