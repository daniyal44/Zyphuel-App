# Zyphuel Error Handling & Validation Architecture

> **Current App Version:** `2.6.4.0.0.24 (Build 52)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

Systematic error classification, input validation, and defensive recovery across Zyphuel.

---

## Principles
- **Fail Gracefully & Safely:** Never display raw Java/Kotlin exception stack traces or internal room SQL queries to users.
- **Categorized Error Classification:** Every error maps to a clear user-facing localized string via `SecurityErrorFormatter`.
- **Defensive Recovery:** Network failures during GPS updates or Gemini rate fetches fall back silently to cached room database state.

---

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

### 4. <font color="#10b981"><b>Delivery Gate & Capacity Validations (Build 52)</b></font>
- <font color="#10b981"><b>Operating Hours Rejection:</b> Returns structured message when order is attempted outside official hours (Mon–Thu 08:00–20:00, Fri 08:00–13:00, Sat–Sun 10:00–18:00 PKT), detailing the next opening time.</font>
- <font color="#10b981"><b>Capacity Overrun Rejection:</b> Intercepts orders $> 15\text{L}$ with explicit maximum volume cap notification.</font>
- <font color="#10b981"><b>Product Availability Guard:</b> Prevents ordering of Water or LPG Gas with an explicit unavailable status notification.</font>

---

## UI Error Notification Pipeline
- Single shared `_uiMessage: MutableStateFlow<String?>` in `MainViewModel`.
- Consumed by `SnackbarHost` across all top-level Compose scaffolds.
- Transient error states auto-dismiss or provide retry actions.
