export type ChatRole = 'user' | 'assistant';

export type PromptStrategy =
  | 'GENERAL'
  | 'DOMAIN_EXPERT'
  | 'CONCISE'
  | 'DETAILED'
  | 'FEW_SHOT'
  | 'STRUCTURED';

export interface ChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  createdAt: string;
  strategy?: PromptStrategy;
}

export const PROMPT_STRATEGIES: { value: PromptStrategy; label: string; hint: string }[] = [
  { value: 'GENERAL', label: 'General', hint: 'Helpful default assistant' },
  { value: 'DOMAIN_EXPERT', label: 'Domain Expert', hint: 'PM/issue-tracking specialist' },
  { value: 'CONCISE', label: 'Concise', hint: 'Short direct answers' },
  { value: 'DETAILED', label: 'Detailed', hint: 'Step-by-step with examples' },
  { value: 'FEW_SHOT', label: 'Few Shot', hint: 'Guided by worked examples' },
  { value: 'STRUCTURED', label: 'Structured', hint: 'JSON-shaped response' },
];
