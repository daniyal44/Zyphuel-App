# Zyphuel Project Rules & Maintenance Guidelines

## Documentation Maintenance Rule (CRITICAL)
Whenever you create, modify, or update any feature, screen, ViewModel method, or database model in Zyphuel:
- **MUST ALWAYS Update `FEATURES_DOCUMENTATION.md`**: Ensure new functions, UI components (such as maps, modals, tracking cards), and permissions are documented in `FEATURES_DOCUMENTATION.md`.
- Keep descriptions clear, concise, and structured with functional details.

## UI & Architecture Rules
- Language: Kotlin with Jetpack Compose (Material 3).
- State Management: `MainViewModel` with `StateFlow`.
- Test Tags: Always include `testTag` for key interactive UI elements.
- Room DB: Retain `UserEntity`, `OrderEntity`, `AuditLogEntity` definitions.
