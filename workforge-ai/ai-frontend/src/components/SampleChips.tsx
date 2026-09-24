interface Props {
  label?: string;
  samples: string[];
  onSelect: (value: string) => void;
  disabled?: boolean;
}

export function SampleChips({ label = 'Samples', samples, onSelect, disabled }: Props) {
  return (
    <div className="space-y-2">
      <p className="text-xs uppercase tracking-wide text-muted">{label}</p>
      <div className="flex flex-wrap gap-2">
        {samples.map((sample) => (
          <button
            key={sample}
            type="button"
            disabled={disabled}
            onClick={() => onSelect(sample)}
            className="rounded-lg border border-border bg-canvas/60 px-3 py-1.5 text-left text-xs text-slate-200 transition hover:border-accent/50 hover:text-accent disabled:opacity-50"
          >
            {sample.length > 72 ? `${sample.slice(0, 69)}…` : sample}
          </button>
        ))}
      </div>
    </div>
  );
}
