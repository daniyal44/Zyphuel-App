package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Compose localization plumbing for Zyphuel.
 *
 * Design principle — **English is the source of truth**: the literal string already written in the
 * code IS the English UI text. The translation dictionary in [AppLanguageManager] only supplies
 * *other* languages. This guarantees the English UI can never regress when a screen is wired for
 * localization (the on-screen literal is always passed as the `english` fallback), and we never need
 * to re-author existing polished English copy.
 *
 * [LocalAppLanguage] is a [staticCompositionLocalOf] so that when the provided language changes at
 * the app root, the *entire* composable subtree recomposes — every wired label re-renders in one go.
 * It is provided (together with `LocalLayoutDirection` for RTL) in `MainActivity`.
 */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguageManager.DEFAULT_LANGUAGE }

/**
 * Translate a UI string for the currently-selected app language.
 *
 * @param key     dictionary key in [AppLanguageManager]'s translation table.
 * @param english the current on-screen English literal — returned verbatim for English, and used as
 *                the ultimate fallback for any language/key that has no dictionary entry.
 */
@Composable
fun tr(key: String, english: String): String {
    val lang = LocalAppLanguage.current
    // English users always see exactly the in-code literal (zero regression).
    if (lang.code == "en") return english
    // Non-English: entry[lang] ?: entry["en"] ?: english  (see AppLanguageManager.translate)
    return AppLanguageManager.translate(key, lang.code, english)
}

/**
 * Translate an order status **for display only**. The stored/compared `order.status` value must stay
 * in English everywhere in logic (e.g. `order.status in listOf("Pending", ...)`); this helper is used
 * solely where a status is rendered to the user.
 *
 * Maps e.g. "Out for Delivery" -> key "status_out_for_delivery"; falls back to the raw status text.
 */
@Composable
fun trStatus(status: String): String =
    tr("status_" + status.lowercase().replace(" ", "_"), status)
