package com.morphingcoffee.gamelauncher.core.telemetry

/**
 * Redacts user-home / install-root prefixes from text before upload.
 */
object PathRedactor {
    fun redact(
        text: String,
        prefixes: Collection<String>,
    ): String {
        if (text.isEmpty() || prefixes.isEmpty()) return text

        val normalizedPrefixes =
            prefixes
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sortedByDescending { it.length }

        var result = text
        for (prefix in normalizedPrefixes) {
            result = replaceIgnoreCase(result, prefix, REDACTED_PATH)
            val forward = prefix.replace('\\', '/')
            if (forward != prefix) {
                result = replaceIgnoreCase(result, forward, REDACTED_PATH)
            }
            val backslash = prefix.replace('/', '\\')
            if (backslash != prefix) {
                result = replaceIgnoreCase(result, backslash, REDACTED_PATH)
            }
        }
        return result
    }

    fun defaultPrefixes(
        userHome: String? = System.getProperty("user.home"),
        appData: String? = System.getenv("APPDATA"),
        userProfile: String? = System.getenv("USERPROFILE"),
        appSupportRoot: String? = null,
    ): List<String> =
        buildList {
            userHome?.takeIf { it.isNotBlank() }?.let(::add)
            appData?.takeIf { it.isNotBlank() }?.let(::add)
            userProfile?.takeIf { it.isNotBlank() }?.let(::add)
            appSupportRoot?.takeIf { it.isNotBlank() }?.let(::add)
        }

    private fun replaceIgnoreCase(
        input: String,
        needle: String,
        replacement: String,
    ): String {
        if (needle.isEmpty()) return input
        val lowerInput = input.lowercase()
        val lowerNeedle = needle.lowercase()
        var searchFrom = 0
        val out = StringBuilder(input.length)
        while (true) {
            val index = lowerInput.indexOf(lowerNeedle, searchFrom)
            if (index < 0) {
                out.append(input, searchFrom, input.length)
                break
            }
            out.append(input, searchFrom, index)
            out.append(replacement)
            searchFrom = index + needle.length
        }
        return out.toString()
    }

    const val REDACTED_PATH = "<redacted>"
}
