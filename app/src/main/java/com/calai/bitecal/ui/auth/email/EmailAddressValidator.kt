package com.calai.bitecal.ui.auth.email

/**
 * Product-level email validator for BiteCal email sign-in.
 *
 * This intentionally does not use android.util.Patterns.EMAIL_ADDRESS so the sign-in UX can enforce
 * BiteCal product rules consistently across Compose UI and ViewModel submission.
 */
object EmailAddressValidator {
    private const val MAX_EMAIL_LENGTH = 254
    private const val MAX_LOCAL_PART_LENGTH = 64
    private val localAllowedRegex = Regex("^[A-Za-z0-9._+-]+$")
    private val domainLabelRegex = Regex("^[A-Za-z0-9-]+$")
    private val tldRegex = Regex("^[A-Za-z]{2,}$")

    fun isValid(rawEmail: String): Boolean {
        val email = rawEmail.trim()
        if (email.length !in 3..MAX_EMAIL_LENGTH) return false

        val atIndex = email.indexOf('@')
        if (atIndex <= 0) return false
        if (atIndex != email.lastIndexOf('@')) return false

        val localPart = email.substring(0, atIndex)
        val domain = email.substring(atIndex + 1)

        if (!isValidLocalPart(localPart)) return false
        return isValidDomain(domain)
    }

    private fun isValidLocalPart(localPart: String): Boolean {
        if (localPart.isBlank()) return false
        if (localPart.length > MAX_LOCAL_PART_LENGTH) return false
        if (!localAllowedRegex.matches(localPart)) return false
        if (localPart.startsWith('.') || localPart.endsWith('.')) return false
        if (localPart.contains("..")) return false
        return true
    }

    private fun isValidDomain(domain: String): Boolean {
        if (domain.isBlank()) return false
        if (domain.contains("..")) return false

        val labels = domain.split('.')
        if (labels.size < 2) return false

        val tld = labels.last()
        if (!tldRegex.matches(tld)) return false

        labels.forEach { label ->
            if (label.isEmpty()) return false
            if (!domainLabelRegex.matches(label)) return false
            if (label.startsWith('-') || label.endsWith('-')) return false
            if (label.contains("--")) return false
        }

        return true
    }
}
