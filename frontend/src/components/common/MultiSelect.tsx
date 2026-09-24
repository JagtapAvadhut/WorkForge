import { useEffect, useRef, useState } from 'react';
import { Check, ChevronDown, X } from 'lucide-react';
import { cn } from '@/utils/cn';

export interface MultiSelectOption {
  value: string;
  label: string;
}

interface MultiSelectProps {
  label?: string;
  placeholder?: string;
  options: MultiSelectOption[];
  value: string[];
  onChange: (value: string[]) => void;
  disabled?: boolean;
}

export function MultiSelect({
  label,
  placeholder = 'Select…',
  options,
  value,
  onChange,
  disabled,
}: MultiSelectProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const toggle = (v: string) =>
    onChange(value.includes(v) ? value.filter((x) => x !== v) : [...value, v]);

  const selected = options.filter((o) => value.includes(o.value));

  return (
    <div className="w-full" ref={ref}>
      {label ? <span className="wf-label">{label}</span> : null}
      <div className="relative">
        <button
          type="button"
          disabled={disabled}
          onClick={() => setOpen((o) => !o)}
          className={cn(
            'wf-input flex min-h-9 items-center justify-between gap-2 text-left',
            disabled && 'cursor-not-allowed opacity-60',
          )}
        >
          <span className="flex flex-1 flex-wrap gap-1">
            {selected.length === 0 ? (
              <span className="text-content-subtle">{placeholder}</span>
            ) : (
              selected.map((o) => (
                <span
                  key={o.value}
                  className="inline-flex items-center gap-1 rounded bg-accent/10 px-1.5 py-0.5 text-xs text-accent"
                >
                  {o.label}
                  <span
                    role="button"
                    tabIndex={-1}
                    onClick={(e) => {
                      e.stopPropagation();
                      toggle(o.value);
                    }}
                    className="hover:text-accent-hover"
                  >
                    <X size={11} />
                  </span>
                </span>
              ))
            )}
          </span>
          <ChevronDown size={16} className="shrink-0 text-content-subtle" />
        </button>

        {open && !disabled ? (
          <div className="absolute z-40 mt-1 max-h-52 w-full overflow-y-auto rounded-md border border-border bg-surface-raised p-1 shadow-popover wf-scrollbar animate-scale-in">
            {options.length === 0 ? (
              <p className="px-2 py-3 text-center text-sm text-content-subtle">No options</p>
            ) : (
              options.map((o) => {
                const active = value.includes(o.value);
                return (
                  <button
                    key={o.value}
                    type="button"
                    onClick={() => toggle(o.value)}
                    className="flex w-full items-center justify-between rounded px-2.5 py-1.5 text-left text-sm text-content hover:bg-canvas"
                  >
                    {o.label}
                    {active ? <Check size={15} className="text-accent" /> : null}
                  </button>
                );
              })
            )}
          </div>
        ) : null}
      </div>
    </div>
  );
}
