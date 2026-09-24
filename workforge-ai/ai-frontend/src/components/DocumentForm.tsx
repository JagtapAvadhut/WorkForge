import { useState, type FormEvent } from 'react';

interface Props {
  disabled?: boolean;
  onSubmit: (content: string, metadata: Record<string, unknown>) => void;
}

export function DocumentForm({ disabled, onSubmit }: Props) {
  const [content, setContent] = useState('');
  const [metadataJson, setMetadataJson] = useState('{"type":"knowledge","topic":"sprint"}');
  const [localError, setLocalError] = useState<string | null>(null);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setLocalError(null);
    let metadata: Record<string, unknown> = {};
    try {
      const parsed = JSON.parse(metadataJson || '{}');
      if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
        throw new Error('Metadata must be a JSON object');
      }
      metadata = parsed as Record<string, unknown>;
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Invalid metadata JSON');
      return;
    }
    if (!content.trim()) {
      setLocalError('Content is required');
      return;
    }
    onSubmit(content.trim(), metadata);
    setContent('');
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-border bg-panel/40 p-5">
      <h2 className="text-lg font-medium text-slate-100">Add document</h2>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={4}
        placeholder="A sprint is a fixed development period used by a team."
        className="w-full rounded-xl border border-border bg-canvas px-4 py-3 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2"
      />
      <label className="block text-xs uppercase tracking-wide text-muted">Metadata (JSON)</label>
      <textarea
        value={metadataJson}
        onChange={(e) => setMetadataJson(e.target.value)}
        rows={2}
        className="w-full rounded-xl border border-border bg-canvas px-4 py-3 font-mono text-xs text-slate-100 outline-none ring-accent/40 focus:ring-2"
      />
      {localError && <p className="text-sm text-red-300">{localError}</p>}
      <button
        type="submit"
        disabled={disabled}
        className="rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-slate-950 disabled:opacity-50"
      >
        {disabled ? 'Saving…' : 'Ingest + embed'}
      </button>
    </form>
  );
}
