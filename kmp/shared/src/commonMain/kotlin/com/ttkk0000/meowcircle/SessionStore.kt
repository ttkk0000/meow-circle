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

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER)
    }

    private companion object {
        const val KEY_TOKEN = "meow.auth.token"
        const val KEY_USER = "meow.auth.user"
    }
}
