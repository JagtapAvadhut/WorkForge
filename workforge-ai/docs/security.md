# Security (Phase 12)

Practical AI security for a **local** WorkForge AI app. Not enterprise-grade.

## Protections

| Area | Behavior |
|------|----------|
| Prompt injection | Heuristic patterns → `SAFE` / `SUSPICIOUS` / `BLOCKED` |
| Tool allowlist | `getIssue`, `searchIssues`, `getProject` only |
| Dangerous tools | delete/update/create/shell/email/payment blocked |
| Input validation | Blank + max length (`max-input-chars`) |
| Secrets in input | JWT/password-like payloads blocked |
| Agent / specialist limits | Configured max iterations / specialist calls |
| MCP allowlist | Same tool allowlist |
| Error sanitization | Paths, JDBC URLs, tokens redacted in API errors |

Central service: `SecurityPolicyService`.

Pipeline: every instrumented AI feature runs `assertAllowedForExecution` before the model/tools.

## APIs

- `POST /api/v1/ai/security/check`
- `GET /api/v1/ai/security/policies`

## What this does NOT guarantee

- Perfect prompt-injection detection (easy to paraphrase around heuristics)
- Protection against a fully compromised host
- Cryptographic confidentiality of chat content
- Authorization / multi-tenant isolation beyond session ids

UI: `/security`
