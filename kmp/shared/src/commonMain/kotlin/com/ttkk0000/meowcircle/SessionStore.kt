package com.ttkk0000.meowcircle

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * Persists auth token and cached user JSON (aligned with Expo `api.ts` keys).
 */
class SessionStore(private val settings: Settings = Settings()) {
    fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)

    fun setToken(value: String?) {
        if (value == null) {
            settings.remove(KEY_TOKEN)
        } else {
            settings[KEY_TOKEN] = value
        }
    }

    fun getUserJson(): String? = settings.getStringOrNull(KEY_USER)

    fun setUserJson(value: String?) {
        if (value == null) {
            settings.remove(KEY_USER)
        } else {
            settings[KEY_USER] = value
        }
    }

    fun getTheme(): String = normalizeTheme(settings.getString(KEY_THEME, "honey"))

    fun setTheme(value: String) {
        settings[KEY_THEME] = normalizeTheme(value)
    }

    fun getProfileBackground(): String = normalizeProfileBackground(settings.getString(KEY_PROFILE_BACKGROUND, "picnic"))

    fun setProfileBackground(value: String) {
        settings[KEY_PROFILE_BACKGROUND] = normalizeProfileBackground(value)
    }

    fun getApiUrl(fallback: String): String = settings.getString(KEY_API_URL, fallback)

    fun setApiUrl(value: String?) {
        if (value == null) {
            settings.remove(KEY_API_URL)
        } else {
            settings[KEY_API_URL] = value
        }
    }

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER)
    }

    private companion object {
        const val KEY_TOKEN = "meow.auth.token"
        const val KEY_USER = "meow.auth.user"
        const val KEY_THEME = "meow.theme"
        const val KEY_PROFILE_BACKGROUND = "meow.profile_background"
        const val KEY_API_URL = "meow.api_url"

        fun normalizeTheme(value: String): String =
            when (value.trim().lowercase()) {
                "honey", "sugar" -> "honey"
                "mint" -> "mint"
                "night" -> "night"
                "neutral", "system" -> "neutral"
                else -> "honey"
            }

        fun normalizeProfileBackground(value: String): String =
            when (value.trim().lowercase()) {
                "picnic" -> "picnic"
                "desk" -> "desk"
                "arcade" -> "arcade"
                "garden" -> "garden"
                else -> "picnic"
            }
    }
}
