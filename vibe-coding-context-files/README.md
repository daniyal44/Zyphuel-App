# Zyphuel Vibe Coding Context Files

These markdown files provide AI coding agents (Antigravity, Claude Code, Cursor, Windsurf) with comprehensive, persistent architecture and operational context for the **Zyphuel** Android application.

## The Context Files
| # | File | What It Locks Down |
|---|------|---------------------|
| 1 | `architecture.md` | Android Jetpack Compose + MVVM solution design, StateFlow reactive pipelines, APIs, and dependencies |
| 2 | `phases.md` | Zyphuel development phases from MVP to Phase 4 Petrol Pump Rate Engine |
| 3 | `database.md` | Room SQLite schema (`users`, `orders`, `audit_logs`, `notifications`, `marked_locations`, `vehicles`) |
| 4 | `prompts.md` | Google Gemini 2.0 Flash Grounding Prompts, AI Assistant system and task prompts |
| 5 | `security.md` | AES-256 GCM encrypted storage, Biometrics, Root/Frida detection, OGRA compliance & Play Store Data Safety |
| 6 | `error-handling.md` | SecurityErrorFormatter, ValidationResult, AppError hierarchy, and UI notification contracts |
| 7 | `generator-prompt.md` | Base template prompt for generating project context files |

## The Golden Rule
When any AI proposes something that contradicts these specifications, the **context files win** — update the code to maintain consistency with the verified Zyphuel architecture.
