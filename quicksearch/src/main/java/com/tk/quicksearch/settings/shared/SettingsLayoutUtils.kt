package com.tk.quicksearch.settings.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet

private const val TABLET_SETTINGS_CONTENT_WIDTH_FRACTION = 0.70f
private const val TABLET_LANDSCAPE_SETTINGS_CONTENT_WIDTH_FRACTION = 0.50f

@Composable
fun Modifier.settingsContentWidth(): Modifier =
    if (isTablet()) {
        if (isLandscape()) {
            fillMaxWidth(TABLET_LANDSCAPE_SETTINGS_CONTENT_WIDTH_FRACTION)
        } else {
            fillMaxWidth(TABLET_SETTINGS_CONTENT_WIDTH_FRACTION)
        }
    } else {
        fillMaxWidth()
    }
