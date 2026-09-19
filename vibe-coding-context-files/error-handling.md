# Zyphuel Error Handling & Validation Architecture

> Systematic error classification, input validation, and defensive recovery across Zyphuel.

## Principles
- **Fail Gracefully & Safely:** Never display raw Java/Kotlin exception stack traces or internal room SQL queries to users.
- **Categorized Error Classification:** Every error maps to a clear user-facing localized string via `SecurityErrorFormatter`.
- **Defensive Recovery:** Network failures during GPS updates or Gemini rate fetches fall back silently to cached room database state.

## Core Validation Classes

### 1. `SecurityInputValidator`
- Validates user emails, international Pakistani phone numbers (`+92 3XX XXXXXXX`), and street addresses.
- Strips potential SQL injection, XSS characters, and script tags before processing.

### 2. `SecurityErrorFormatter`
- Converts network IO exceptions, auth failures, and rate limit breaches into clean, branded Urdu/English error dialogues.

### 3. `ValidationResult`
- Standard data class wrapper:
  ```kotlin
  data class ValidationResult(
      val isValid: Boolean,
      val errorMessage: String? = null
  )
  ```

## UI Error Notification Pipeline
- Single shared `_uiMessage: MutableStateFlow<String?>` in `MainViewModel`.
- Consumed by `SnackbarHost` across all top-level Compose scaffolds.
- Transient error states auto-dismiss or provide retry actions.
