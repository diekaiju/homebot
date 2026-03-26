package com.tk.quicksearch.searchEngines

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchTarget

sealed interface AliasTarget {
    data class Search(val target: SearchTarget) : AliasTarget

    data class Section(val section: SearchSection) : AliasTarget

    data class Feature(val featureId: String) : AliasTarget
}

fun AliasTarget.asSearchTargetOrNull(): SearchTarget? =
    when (this) {
        is AliasTarget.Search -> target
        is AliasTarget.Section -> null
        is AliasTarget.Feature -> null
    }
