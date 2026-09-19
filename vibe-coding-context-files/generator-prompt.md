# Generator Prompt

> Template prompt to generate context files for other projects or submodules.

```
You are a senior software architect. I'm building this app:

<<< DESCRIBE YOUR APP HERE >>>
- What it does:
- Who it's for:
- Core feature (the one thing it must nail):
- Tech I want to use (or say "you choose"):
- Any AI/LLM features:
- Scale for v1 (rough):

Generate SIX markdown files as my project's source of truth:
1. architecture.md  — overview, tech stack (table + why), data-flow diagram, full API list, integrations, constraints, open questions.
2. phases.md        — split the build into Phase 1 (MVP) → Phase 4 (polish/scale). Each phase must be usable on its own.
3. database.md      — full schema: every table with columns, types, keys, relationships, indexes, and conventions.
4. prompts.md       — for each AI agent in my app: purpose, system prompt, and functional prompts.
5. security.md      — auth, authorization, input validation, secrets, transport/headers, rate limiting, and a pre-launch checklist.
6. error-handling.md — custom error classes, HTTP status code table, a standard success/error response shape, central handler, and logging plan.
```
