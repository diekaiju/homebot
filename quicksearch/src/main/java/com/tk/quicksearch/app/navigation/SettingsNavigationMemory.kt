package com.tk.quicksearch.app.navigation

import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType

object SettingsNavigationMemory {
    private var lastOpenedSettingsDetailType: SettingsDetailType? = null

    fun rememberSettingsDetail(detailType: SettingsDetailType) {
        lastOpenedSettingsDetailType = detailType
    }

    fun getLastOpenedSettingsDetail(): SettingsDetailType? = lastOpenedSettingsDetailType
}
