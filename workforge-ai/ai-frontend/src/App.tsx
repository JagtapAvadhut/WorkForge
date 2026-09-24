import { Navigate, Route, Routes } from 'react-router-dom';
import AgentPage from './pages/AgentPage';
import AiChatPage from './pages/AiChatPage';
import DocumentsPage from './pages/DocumentsPage';
import EmbeddingsPage from './pages/EmbeddingsPage';
import GraphAgentPage from './pages/GraphAgentPage';
import McpPage from './pages/McpPage';
import MemoryPage from './pages/MemoryPage';
import MultiAgentPage from './pages/MultiAgentPage';
import ObservabilityPage from './pages/ObservabilityPage';
import EvaluationPage from './pages/EvaluationPage';
import SecurityPage from './pages/SecurityPage';
import RagPage from './pages/RagPage';
import ToolsPage from './pages/ToolsPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/ai-chat" replace />} />
      <Route path="/ai-chat" element={<AiChatPage />} />
      <Route path="/embeddings" element={<EmbeddingsPage />} />
      <Route path="/documents" element={<DocumentsPage />} />
      <Route path="/rag" element={<RagPage />} />
      <Route path="/tools" element={<ToolsPage />} />
      <Route path="/agent" element={<AgentPage />} />
      <Route path="/graph-agent" element={<GraphAgentPage />} />
      <Route path="/mcp" element={<McpPage />} />
      <Route path="/memory" element={<MemoryPage />} />
      <Route path="/multi-agent" element={<MultiAgentPage />} />
      <Route path="/evaluation" element={<EvaluationPage />} />
      <Route path="/security" element={<SecurityPage />} />
      <Route path="/observability" element={<ObservabilityPage />} />
      <Route path="*" element={<Navigate to="/ai-chat" replace />} />
    </Routes>
  );
}
