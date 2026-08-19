package com.example.security

import java.util.regex.Pattern

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}

object SecurityInputValidator {

    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,10}\$"
    )

    private val PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9]{10,15}\$"
    )

    private val CNIC_PATTERN = Pattern.compile(
        "^[0-9]{5}-?[0-9]{7}-?[0-9]{1}\$"
    )

    private val NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z\\s.'-]{2,50}\$"
    )

    /**
     * Strict schema validation for Email Address.
     */
    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Email address cannot be empty.")
        }
        if (trimmed.length < 5 || trimmed.length > 100) {
            return ValidationResult.Invalid("Email must be between 5 and 100 characters in length.")
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.Invalid("Invalid email format. Please enter a valid email address (e.g. user@domain.com).")
        }
        return ValidationResult.Valid
    }

    /**
     * Strict schema validation for Passwords.
     */
    fun validatePassword(password: String): ValidationResult {
        if (password.length < 8) {
            return ValidationResult.Invalid("Password must be at least 8 characters long.")
        }
        if (password.length > 64) {
            return ValidationResult.Invalid("Password cannot exceed 64 characters.")
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        if (!hasLetter || (!hasDigit && !hasSpecial)) {
            return ValidationResult.Invalid("Password must contain letters and at least one number or special character.")
        }
        return ValidationResult.Valid
    }

    /**
     * Strict schema validation for User Full Name.
     */
    fun validateName(name: String, fieldLabel: String = "Full Name"): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("$fieldLabel is required.")
        }
        if (trimmed.length < 2 || trimmed.length > 50) {
            return ValidationResult.Invalid("$fieldLabel must be between 2 and 50 characters.")
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.Invalid("$fieldLabel contains invalid characters. Use letters, spaces, dots, or hyphens.")
        }
        return ValidationResult.Valid
    }

    /**
     * Strict schema validation for Phone numbers.
     */
    fun validatePhone(phone: String): ValidationResult {
        val cleaned = phone.replace(" ", "").replace("-", "").trim()
        if (cleaned.isEmpty()) {
            return ValidationResult.Invalid("Phone number is required.")
        }
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            return ValidationResult.Invalid("Invalid phone format. Must be 10 to 15 digits (e.g. +923001234567 or 03001234567).")
        }
        return ValidationResult.Valid
    }

    /**
     * Strict schema validation for Pakistani CNIC / Document ID.
     */
    fun validateCNIC(cnic: String): ValidationResult {
        val cleaned = cnic.replace("-", "").trim()
        if (cleaned.isEmpty()) {
            return ValidationResult.Invalid("CNIC or Document number is required.")
        }
        if (cleaned.length != 13 || !cleaned.all { it.isDigit() }) {
            return ValidationResult.Invalid("Invalid CNIC number. Must contain exactly 13 digits (e.g. 35202-1234567-1).")
        }
        return ValidationResult.Valid
    }

    /**
     * Strict schema validation for Delivery Address.
     */
    fun validateAddress(address: String): ValidationResult {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Delivery address cannot be empty.")
        }
        if (trimmed.length < 5 || trimmed.length > 200) {
            return ValidationResult.Invalid("Address must be between 5 and 200 characters long.")
        }
        if (trimmed.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }) {
            return ValidationResult.Invalid("Address contains invalid non-printable control characters.")
        }
        return ValidationResult.Valid
    }

    /**
     * Strict schema validation for Fuel / Service Order Quantities.
     */
    fun validateQuantity(qty: Double, minAllowed: Double = 0.5, maxAllowed: Double = 1000.0, unitName: String = "Liters"): ValidationResult {
        if (qty.isNaN() || qty.isInfinite()) {
            return ValidationResult.Invalid("Invalid quantity value.")
        }
        if (qty < minAllowed) {
            return ValidationResult.Invalid("Minimum order quantity is $minAllowed $unitName.")
        }
        if (qty > maxAllowed) {
            return ValidationResult.Invalid("Maximum single order quantity is $maxAllowed $unitName.")
        }
        return ValidationResult.Valid
    }

    /**
     * Strict schema validation for Ratings and Feedback Comments.
     */
    fun validateRatingAndFeedback(rating: Int, feedback: String): ValidationResult {
        if (rating !in 1..5) {
            return ValidationResult.Invalid("Rating must be between 1 and 5 stars.")
        }
        val trimmed = feedback.trim()
        if (trimmed.length > 500) {
            return ValidationResult.Invalid("Feedback comment cannot exceed 500 characters.")
        }
        return ValidationResult.Valid
    }
}
