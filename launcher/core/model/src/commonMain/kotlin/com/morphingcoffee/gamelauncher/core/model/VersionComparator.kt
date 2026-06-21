package com.morphingcoffee.gamelauncher.core.model

/**
 * Compares launcher version strings such as `0.0.1`, `0.0.1-build51`, and `0.0.2-build1`.
 *
 * Order: marketing semver (major.minor.patch), then optional `-buildN` suffix (missing suffix = build 0).
 */
object VersionComparator {
    fun compare(
        left: String,
        right: String,
    ): Int {
        val leftParts = parse(left)
        val rightParts = parse(right)

        val marketingCompare =
            compareValuesBy(
                leftParts,
                rightParts,
                { it.major },
                { it.minor },
                { it.patch },
            )
        if (marketingCompare != 0) return marketingCompare

        return leftParts.buildNumber.compareTo(rightParts.buildNumber)
    }

    fun isLessThan(
        runtime: String,
        remote: String,
    ): Boolean = compare(runtime, remote) < 0

    fun isLessThanOrEqual(
        runtime: String,
        remote: String,
    ): Boolean = compare(runtime, remote) <= 0

    private data class ParsedVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val buildNumber: Int,
    )

    private fun parse(raw: String): ParsedVersion {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Version must not be blank" }

        val withoutDev = trimmed.removeSuffix("-dev")
        val buildIndex = withoutDev.indexOf("-build")
        val marketingPart =
            if (buildIndex >= 0) {
                withoutDev.substring(0, buildIndex)
            } else {
                withoutDev
            }
        val buildNumber =
            if (buildIndex >= 0) {
                withoutDev.substring(buildIndex + "-build".length).toIntOrNull()
                    ?: error("Invalid build suffix in version: $raw")
            } else {
                0
            }

        val segments = marketingPart.split('.')
        require(segments.size == 3) { "Expected major.minor.patch version: $raw" }

        return ParsedVersion(
            major = segments[0].toIntOrNull() ?: error("Invalid major in version: $raw"),
            minor = segments[1].toIntOrNull() ?: error("Invalid minor in version: $raw"),
            patch = segments[2].toIntOrNull() ?: error("Invalid patch in version: $raw"),
            buildNumber = buildNumber,
        )
    }
}
