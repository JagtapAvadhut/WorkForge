import { Monitor, Moon, Sun } from 'lucide-react';
import { useUiStore, type ThemeMode } from '@/stores/uiStore';
import { cn } from '@/utils/cn';

const options: { value: ThemeMode; icon: typeof Sun; label: string }[] = [
  { value: 'light', icon: Sun, label: 'Light' },
  { value: 'dark', icon: Moon, label: 'Dark' },
  { value: 'system', icon: Monitor, label: 'System' },
];

export function ThemeToggle() {
  const theme = useUiStore((s) => s.theme);
  const setTheme = useUiStore((s) => s.setTheme);

  return (
    <div
      className="inline-flex items-center gap-0.5 rounded-lg border border-border bg-surface p-0.5"
      role="radiogroup"
      aria-label="Theme"
    >
      {options.map(({ value, icon: Icon, label }) => (
        <button
          key={value}
          type="button"
          role="radio"
          aria-checked={theme === value}
          title={label}
          onClick={() => setTheme(value)}
          className={cn(
            'flex h-7 w-7 items-center justify-center rounded-md transition-colors',
            theme === value
              ? 'bg-accent/10 text-accent'
              : 'text-content-subtle hover:text-content',
          )}
        >
          <Icon size={15} />
        </button>
      ))}
    </div>
  );
}
