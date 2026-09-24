import { PROMPT_STRATEGIES, type PromptStrategy } from '@/types/chat';

interface Props {
  value: PromptStrategy;
  onChange: (strategy: PromptStrategy) => void;
  disabled?: boolean;
}

export function StrategySelector({ value, onChange, disabled }: Props) {
  const current = PROMPT_STRATEGIES.find((s) => s.value === value);

  return (
    <div className="flex flex-wrap items-center gap-3">
      <label className="text-xs font-medium uppercase tracking-wide text-muted" htmlFor="prompt-strategy">
        Prompt strategy
      </label>
      <select
        id="prompt-strategy"
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value as PromptStrategy)}
        className="rounded-lg border border-border bg-canvas px-3 py-2 text-sm text-slate-100 outline-none ring-accent/40 focus:ring-2 disabled:opacity-60"
      >
        {PROMPT_STRATEGIES.map((s) => (
          <option key={s.value} value={s.value}>
            {s.label}
          </option>
        ))}
      </select>
      <span className="rounded-full border border-accent/40 bg-accent/10 px-3 py-1 font-mono text-xs text-accent">
        {value}
      </span>
      {current && <span className="text-xs text-muted">{current.hint}</span>}
    </div>
  );
}
