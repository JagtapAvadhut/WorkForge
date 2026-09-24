import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { FolderKanban, Search, User as UserIcon } from 'lucide-react';
import { searchApi } from '@/api/searchApi';
import { useDebounce } from '@/hooks/useDebounce';
import { Avatar } from '@/components/ui/Avatar';
import { Spinner } from '@/components/ui/Spinner';
import { IssueTypeIcon } from '@/components/issues/IssueTypeIcon';
import { cn } from '@/utils/cn';

export function GlobalSearch() {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const debounced = useDebounce(query.trim(), 300);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

  const enabled = debounced.length >= 2;
  const { data, isFetching } = useQuery({
    queryKey: ['search', debounced],
    queryFn: () => searchApi.global(debounced),
    enabled,
  });

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        inputRef.current?.focus();
        setOpen(true);
      }
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, []);

  const go = (path: string) => {
    navigate(path);
    setOpen(false);
    setQuery('');
  };

  const hasResults =
    data && (data.issues.length > 0 || data.projects.length > 0 || data.users.length > 0);

  return (
    <div ref={containerRef} className="relative w-full max-w-md">
      <div className="relative">
        <Search
          size={16}
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-content-subtle"
          aria-hidden
        />
        <input
          ref={inputRef}
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          placeholder="Search issues, projects, people…"
          aria-label="Global search"
          className="h-9 w-full rounded-md border border-border bg-surface pl-9 pr-16 text-sm text-content placeholder:text-content-subtle focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
        />
        <kbd className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 rounded border border-border bg-canvas px-1.5 py-0.5 font-mono text-[10px] text-content-subtle">
          ⌘K
        </kbd>
      </div>

      {open && enabled ? (
        <div className="absolute left-0 right-0 top-full z-40 mt-2 max-h-[70vh] overflow-y-auto rounded-lg border border-border bg-surface-raised p-2 shadow-popover wf-scrollbar animate-scale-in">
          {isFetching ? (
            <div className="flex items-center justify-center py-6">
              <Spinner label="Searching…" />
            </div>
          ) : !hasResults ? (
            <p className="px-3 py-6 text-center text-sm text-content-muted">
              No results for “{debounced}”.
            </p>
          ) : (
            <div className="space-y-3">
              {data!.issues.length > 0 && (
                <SearchGroup label="Issues">
                  {data!.issues.map((issue) => (
                    <button
                      key={issue.id}
                      type="button"
                      onClick={() => go(`/issues/${issue.key}`)}
                      className="flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-left hover:bg-canvas"
                    >
                      <IssueTypeIcon type={issue.type} size={15} />
                      <span className="font-mono text-xs text-content-subtle">{issue.key}</span>
                      <span className="min-w-0 flex-1 truncate text-sm text-content">
                        {issue.summary}
                      </span>
                    </button>
                  ))}
                </SearchGroup>
              )}
              {data!.projects.length > 0 && (
                <SearchGroup label="Projects">
                  {data!.projects.map((project) => (
                    <button
                      key={project.id}
                      type="button"
                      onClick={() => go(`/projects/${project.key}`)}
                      className="flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-left hover:bg-canvas"
                    >
                      <FolderKanban size={15} className="text-content-subtle" />
                      <span className="font-mono text-xs text-content-subtle">{project.key}</span>
                      <span className="min-w-0 flex-1 truncate text-sm text-content">
                        {project.name}
                      </span>
                    </button>
                  ))}
                </SearchGroup>
              )}
              {data!.users.length > 0 && (
                <SearchGroup label="People">
                  {data!.users.map((user) => (
                    <div
                      key={user.id}
                      className="flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-left"
                    >
                      <Avatar user={user} size={20} />
                      <span className="min-w-0 flex-1 truncate text-sm text-content">
                        {user.fullName}
                      </span>
                      <UserIcon size={13} className="text-content-subtle" />
                    </div>
                  ))}
                </SearchGroup>
              )}
            </div>
          )}
        </div>
      ) : null}
    </div>
  );
}

function SearchGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className={cn('px-2.5 pb-1 text-[11px] font-semibold uppercase tracking-wide text-content-subtle')}>
        {label}
      </p>
      {children}
    </div>
  );
}
