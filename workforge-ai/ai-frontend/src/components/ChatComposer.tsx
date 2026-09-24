import { useEffect, useState, type FormEvent } from 'react';

interface Props {
  disabled?: boolean;
  draft?: string;
  onDraftChange?: (value: string) => void;
  onSend: (message: string) => void;
}

export function ChatComposer({ disabled, draft, onDraftChange, onSend }: Props) {
  const [internal, setInternal] = useState('');
  const controlled = draft !== undefined;
  const value = controlled ? draft : internal;

  useEffect(() => {
    if (controlled && draft != null) {
      // parent owns draft
    }
  }, [controlled, draft]);

  function setValue(next: string) {
    if (controlled) onDraftChange?.(next);
    else setInternal(next);
  }

  function submit(e?: FormEvent) {
    e?.preventDefault();
    const trimmed = value.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setValue('');
  }

  return (
    <form onSubmit={submit} className="flex gap-3 border-t border-border bg-panel/80 p-4 backdrop-blur">
      <input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            submit();
          }
        }}
        disabled={disabled}
        placeholder="Ask about projects, issues, sprints, workflows…"
        className="flex-1 rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 placeholder:text-muted focus:ring-2 disabled:opacity-60"
      />
      <button
        type="submit"
        disabled={disabled || !value.trim()}
        className="rounded-xl bg-accent px-5 py-3 text-sm font-semibold text-slate-950 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {disabled ? 'Thinking…' : 'Send'}
      </button>
    </form>
  );
}
