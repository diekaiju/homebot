package com.tk.quicksearch.search.fuzzy

/**
 * Configuration for fuzzy search behavior.
 * Centralized configuration to avoid scattering across multiple files.
 */
data class FuzzySearchConfig(
    val matchThreshold: Int = 75,
    val minQueryLength: Int = 3,
    val priority: Int = 5,
) {
    companion object {
        /**
         * Default configuration for app search.
         */
        val DEFAULT_APP_CONFIG =
            FuzzySearchConfig(
                matchThreshold = 75,
                minQueryLength = 3,
                priority = 5,
            )

        /**
         * Creates a config from individual parameters.
         */
        fun create(
            matchThreshold: Int = DEFAULT_APP_CONFIG.matchThreshold,
            minQueryLength: Int = DEFAULT_APP_CONFIG.minQueryLength,
            priority: Int = DEFAULT_APP_CONFIG.priority,
        ) = FuzzySearchConfig(matchThreshold, minQueryLength, priority)
    }
}
