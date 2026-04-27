package com.vandoliak.coupleapp.data.local

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.name == value } ?: SYSTEM
        }
    }
}

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    UKRAINIAN("uk");

    companion object {
        fun fromCode(value: String?): AppLanguage {
            return entries.firstOrNull { it.code == value } ?: ENGLISH
        }
    }
}

enum class AppCurrency(val symbol: String, val suffix: Boolean) {
    UAH("\u20B4", true),
    USD("$", false),
    EUR("\u20AC", false),
    SEK("kr", true);

    companion object {
        fun fromValue(value: String?): AppCurrency {
            return entries.firstOrNull { it.name == value } ?: UAH
        }
    }
}

class AppSettingsManager(private val context: Context) {

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val LANGUAGE_KEY = stringPreferencesKey("language_code")
    private val CURRENCY_KEY = stringPreferencesKey("currency_code")

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromValue(prefs[THEME_MODE_KEY])
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[LANGUAGE_KEY])
    }

    val currencyFlow: Flow<AppCurrency> = context.dataStore.data.map { prefs ->
        AppCurrency.fromValue(prefs[CURRENCY_KEY])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language.code
        }
    }

    suspend fun setCurrency(currency: AppCurrency) {
        context.dataStore.edit { prefs ->
            prefs[CURRENCY_KEY] = currency.name
        }
    }

    suspend fun currentLanguage(): AppLanguage {
        return languageFlow.first()
    }

    fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.code)
        )
    }
}
