package com.tk.quicksearch.shared.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun TextFieldValue.withoutWhitespaces(): TextFieldValue {
    val filteredText = text.filterNot(Char::isWhitespace)
    if (filteredText == text) return this
    val filteredSelectionStart =
        text.take(selection.start.coerceAtMost(text.length)).count { !it.isWhitespace() }
    val filteredSelectionEnd =
        text.take(selection.end.coerceAtMost(text.length)).count { !it.isWhitespace() }
    return copy(
        text = filteredText,
        selection = TextRange(filteredSelectionStart, filteredSelectionEnd),
    )
}
