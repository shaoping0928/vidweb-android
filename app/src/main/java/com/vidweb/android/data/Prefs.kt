package com.vidweb.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vidweb")

class Prefs(private val context: Context) {
    private val serverKey = stringPreferencesKey("server_url")

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[serverKey] ?: DEFAULT_SERVER
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[serverKey] = ApiClient.normalize(url)
        }
    }

    companion object {
        const val DEFAULT_SERVER = "http://127.0.0.1:8765/"
    }
}
