import { NavLink } from 'react-router-dom';

export function AppNav() {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `rounded-lg px-3 py-1.5 text-sm transition ${
      isActive ? 'bg-accent/20 text-accent' : 'text-muted hover:text-slate-100'
    }`;

  return (
    <nav className="mb-4 flex flex-wrap items-center gap-2 border-b border-border pb-3">
      <span className="mr-2 font-mono text-xs uppercase tracking-[0.2em] text-accent">WorkForge AI</span>
      <NavLink to="/ai-chat" className={linkClass}>
        Chat
      </NavLink>
      <NavLink to="/embeddings" className={linkClass}>
        Embeddings
      </NavLink>
      <NavLink to="/documents" className={linkClass}>
        Documents
      </NavLink>
      <NavLink to="/rag" className={linkClass}>
        RAG
      </NavLink>
      <NavLink to="/tools" className={linkClass}>
        Tools
      </NavLink>
      <NavLink to="/agent" className={linkClass}>
        Agent
      </NavLink>
      <NavLink to="/graph-agent" className={linkClass}>
        Graph
      </NavLink>
      <NavLink to="/mcp" className={linkClass}>
        MCP
      </NavLink>
      <NavLink to="/memory" className={linkClass}>
        Memory
      </NavLink>
      <NavLink to="/multi-agent" className={linkClass}>
        Multi-Agent
      </NavLink>
      <NavLink to="/evaluation" className={linkClass}>
        Evaluation
      </NavLink>
      <NavLink to="/security" className={linkClass}>
        Security
      </NavLink>
      <NavLink to="/observability" className={linkClass}>
        Observability
      </NavLink>
    </nav>
  );
}
