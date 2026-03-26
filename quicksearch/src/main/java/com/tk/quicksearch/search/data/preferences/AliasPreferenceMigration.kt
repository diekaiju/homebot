package com.tk.quicksearch.search.data.preferences

// TODO: Remove this migration after sufficient time has passed for users to migrate from shortcuts to aliases
internal object AliasPreferenceMigration {
    fun resolveAliasValue(
        aliasValue: String?,
        legacyShortcutValue: String?,
    ): String? = aliasValue ?: legacyShortcutValue
}
