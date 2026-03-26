package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal fun Modifier.predictedSubmitHighlight(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
): Modifier =
    composed {
        val indicatorAlpha =
            animateFloatAsState(
                targetValue = if (isPredicted) 1f else 0f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "predictedSubmitIndicatorAlpha",
            ).value

        if (indicatorAlpha <= 0f) {
            this
        } else {
            this
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f * indicatorAlpha),
                    shape = shape,
                )
                .border(
                    width = DesignTokens.BorderWidth,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f * indicatorAlpha),
                    shape = shape,
                )
        }
    }

internal fun Modifier.predictedSubmitCardBorder(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
): Modifier =
    composed {
        if (!isPredicted) {
            this
        } else {
            this.border(
                width = DesignTokens.BorderWidth,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                shape = shape,
            )
        }
    }
