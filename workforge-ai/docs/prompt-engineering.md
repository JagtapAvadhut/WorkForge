# Prompt Engineering (Phase 2)

How WorkForge AI builds prompts before calling Ollama.

## 1. System prompt

Sets the assistant persona and shared constraints. Chosen by `strategy`.

**Request**

```json
POST /api/v1/ai/chat
{ "message": "Explain sprint", "strategy": "DOMAIN_EXPERT" }
```

**What the model receives (system)** — enterprise PM expert + shared constraints (no invented live data).

**Response**

```json
{
  "success": true,
  "data": {
    "response": "A sprint is a fixed-length iteration...",
    "strategy": "DOMAIN_EXPERT"
  }
}
```

Inspect without calling the model:

```json
POST /api/v1/ai/prompt-preview
{ "message": "Explain sprint", "strategy": "DOMAIN_EXPERT" }
```

## 2. User prompt

Always rendered from a Spring AI `PromptTemplate`:

```text
You are a <role>.
Explain <topic> for a <audience>.
Use <style> style.

User question:
<question>
```

**Request**

```json
{ "message": "Explain sprint", "strategy": "CONCISE" }
```

Role/audience/style change with strategy; the question is your `message`.

## 3. Prompt template

Templates live in `WorkforgePromptTemplates` and are rendered by `PromptTemplateService` using Spring AI `PromptTemplate` + `StTemplateRenderer` (`<` / `>` delimiters). Controllers never concatenate prompt strings.

## 4. Variables

| Variable | Meaning |
|----------|---------|
| `role` | Persona for this strategy |
| `topic` | Short form of the user message |
| `audience` | Intended reader |
| `style` | Writing style |
| `question` | Full user message |
| `constraints` | Shared safety/scope rules injected into every system prompt |

## 5. Few-shot prompting

`strategy: "FEW_SHOT"` prepends reusable examples (sprint, backlog) as user/assistant message pairs before history and the live question.

```json
{ "message": "What is a board?", "strategy": "FEW_SHOT" }
```

Preview shows those example turns under `data.messages`.

## 6. Constraints

Shared block in every system prompt:

- Stay on topic
- Do not invent WorkForge data
- Do not claim DB/API access
- State uncertainty clearly
- Prefer simple language

## 7. Output instructions

- **CONCISE** — 1–3 short sentences
- **DETAILED** — definition → how it works → example → pitfalls
- **STRUCTURED** — JSON only:

```json
{
  "summary": "...",
  "keyPoints": ["..."],
  "example": "...",
  "caveat": "..."
}
```

Example:

```json
{ "message": "Explain epic", "strategy": "STRUCTURED" }
```

## 8. Conversation context

Optional `history` (user/assistant only). Backend keeps the last `workforge.ai.chat.max-history-messages` (default 10). No database memory.

```json
{
  "message": "How does that relate to a board?",
  "strategy": "GENERAL",
  "history": [
    { "role": "user", "content": "What is a sprint?" },
    { "role": "assistant", "content": "A sprint is a fixed development period..." }
  ]
}
```

## 9. Prompt versioning

In-code versions (no DB):

| Version | Strategy |
|---------|----------|
| `GENERAL_V1` | GENERAL |
| `DOMAIN_EXPERT_V1` | DOMAIN_EXPERT |
| `CONCISE_V1` | CONCISE |
| `DETAILED_V1` | DETAILED |
| `FEW_SHOT_V1` | FEW_SHOT |
| `STRUCTURED_V1` | STRUCTURED |

Returned by `/prompt-preview` as `data.version`.

---

## What changes when the strategy changes?

- System persona and output shape
- Template variables (`role`, `audience`, `style`)
- Whether few-shot examples are injected
- Prompt version id

## What stays the same?

- Endpoint path and envelope (`success` / `data`)
- Ollama model + Spring AI `ChatClient`
- Shared constraints
- Optional history trimming
- No live WorkForge DB access

## What is handled by Spring AI vs the LLM?

| Layer | Responsibility |
|-------|----------------|
| **Your code** | Choose strategy, load templates, fill variables, attach few-shot + history |
| **Spring AI** | `PromptTemplate` render, message assembly, `ChatClient` call to Ollama |
| **LLM (Ollama)** | Generate the natural-language (or JSON) answer |

Use `/api/v1/ai/prompt-preview` to study Spring AI’s message list before the model runs.
