package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.SearchEngine

/**
 * Backward-compatible wrapper. New code should use [AliasPreferences].
 */
@Deprecated("Use AliasPreferences instead.")
class ShortcutPreferences(
    context: Context,
) : AliasPreferences(context) {
    fun areShortcutsEnabled(): Boolean = areAliasesEnabled()

    fun setShortcutsEnabled(enabled: Boolean) = setAliasesEnabled(enabled)

    fun getShortcutCode(engine: SearchEngine): String = getAliasCode(engine)

    fun setShortcutCode(
        engine: SearchEngine,
        code: String,
    ) = setAliasCode(engine, code)

    fun getShortcutCode(targetId: String): String? = getAliasCode(targetId)

    fun setShortcutCode(
        targetId: String,
        code: String,
    ) = setAliasCode(targetId, code)

    fun isShortcutEnabled(engine: SearchEngine): Boolean = isAliasEnabled(engine)

    fun setShortcutEnabled(
        engine: SearchEngine,
        enabled: Boolean,
    ) = setAliasEnabled(engine, enabled)

    fun getAllShortcutCodes(): Map<SearchEngine, String> = getAllAliasCodes()
}
