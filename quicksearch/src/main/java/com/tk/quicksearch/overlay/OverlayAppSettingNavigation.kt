package com.tk.quicksearch.overlay

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.app.navigation.toSettingsDetailTypeOrNull
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.util.FeedbackUtils

private const val QUICK_SEARCH_DEVELOPMENT_URL = "https://github.com/teja2495/quick-search"

internal fun handleOverlayAppSettingDestination(
    context: Context,
    destination: AppSettingsDestination,
    viewModel: SearchViewModel,
    autoCloseOverlay: Boolean,
    onCloseRequested: () -> Unit,
) {
    destination.toSettingsDetailTypeOrNull()?.let { detailType ->
        openOverlaySettingsDetail(context, detailType, onCloseRequested)
        return
    }

    val closeIfNeeded = {
        if (autoCloseOverlay) onCloseRequested()
    }

    when (destination) {
        AppSettingsDestination.RELOAD_APPS -> viewModel.refreshApps(showToast = true)
        AppSettingsDestination.RELOAD_CONTACTS -> viewModel.refreshContacts(showToast = true)
        AppSettingsDestination.RELOAD_FILES -> viewModel.refreshFiles(showToast = true)
        AppSettingsDestination.SEND_FEEDBACK -> {
            FeedbackUtils.launchFeedbackEmail(context = context, feedbackText = null)
            closeIfNeeded()
        }
        AppSettingsDestination.RATE_QUICK_SEARCH -> {
            launchRateQuickSearch(context)
            closeIfNeeded()
        }
        AppSettingsDestination.DEVELOPMENT -> {
            launchDevelopmentPage(context)
            closeIfNeeded()
        }
        AppSettingsDestination.SET_DEFAULT_ASSISTANT -> {
            try {
                context.startActivity(Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS))
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_unable_to_open_settings),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            closeIfNeeded()
        }
        AppSettingsDestination.ADD_HOME_SCREEN_WIDGET -> {
            com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget(context)
            closeIfNeeded()
        }
        AppSettingsDestination.ADD_QUICK_SETTINGS_TILE -> {
            com.tk.quicksearch.tile.requestAddQuickSearchTile(context)
            closeIfNeeded()
        }
        else -> Unit
    }
}

private fun openOverlaySettingsDetail(
    context: Context,
    detailType: SettingsDetailType,
    onCloseRequested: () -> Unit,
) {
    OverlayModeController.openMainActivity(
        context = context,
        openSettings = true,
        settingsDetailType = detailType,
    )
    onCloseRequested()
}

private fun launchRateQuickSearch(context: Context) {
    val packageName = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            },
        )
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ),
            )
        }
    }
}

private fun launchDevelopmentPage(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(QUICK_SEARCH_DEVELOPMENT_URL)))
    }
}
