package com.tk.quicksearch.search.searchScreen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun ExcludeUndoSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        snackbar = { data ->
            val message = data.visuals.message
            val marker = stringResource(R.string.exclude_marker)
            val markerIndex = message.indexOf(marker)
            val annotatedMessage =
                if (markerIndex > 0) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(message.substring(0, markerIndex))
                        }
                        append(message.substring(markerIndex))
                    }
                } else {
                    AnnotatedString(message)
                }
            val actionLabel = data.visuals.actionLabel
            Snackbar(
                action =
                    actionLabel?.let { label ->
                        {
                            TextButton(onClick = { data.performAction() }) {
                                Text(text = label)
                            }
                        }
                    },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                actionContentColor = MaterialTheme.colorScheme.primary,
                shape = DesignTokens.ShapeLarge,
            ) {
                Text(
                    text = annotatedMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = modifier,
    )
}