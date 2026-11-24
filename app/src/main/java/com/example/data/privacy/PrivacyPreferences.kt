package com.example.data.privacy

import android.content.Context

class PrivacyPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "harvera_privacy",
        Context.MODE_PRIVATE
    )

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    private companion object {
        const val KEY_ENABLED = "privacy_mode_enabled"
    }
}
