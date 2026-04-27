package com.vandoliak.coupleapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("auth_prefs")

class TokenManager(private val context: Context) {

    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val PAIR_ID_KEY = stringPreferencesKey("pair_id")

    val tokenFlow = context.dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    val pairIdFlow = context.dataStore.data.map { prefs ->
        prefs[PAIR_ID_KEY]
    }

    suspend fun saveSession(token: String, pairId: String?) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            if (pairId.isNullOrBlank()) {
                prefs.remove(PAIR_ID_KEY)
            } else {
                prefs[PAIR_ID_KEY] = pairId
            }
        }
    }

    suspend fun savePairId(pairId: String) {
        context.dataStore.edit { prefs ->
            prefs[PAIR_ID_KEY] = pairId
        }
    }

    suspend fun clearPairId() {
        context.dataStore.edit { prefs ->
            prefs.remove(PAIR_ID_KEY)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(PAIR_ID_KEY)
        }
    }
}
