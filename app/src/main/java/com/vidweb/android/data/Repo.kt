package com.vidweb.android.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class Repo(
    context: Context,
    private val scope: CoroutineScope,
) {
    val prefs = Prefs(context)

    private val _state = MutableStateFlow<Map<String, String>>(emptyMap())
    val state: StateFlow<Map<String, String>> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun loadState() {
        runCatching { ApiClient.api.state().items }.onSuccess { _state.value = it }
    }

    fun get(key: String, fallback: String = ""): String = _state.value[key] ?: fallback

    fun getBool(key: String, fallback: Boolean): Boolean {
        val raw = _state.value[key] ?: return fallback
        return raw == "1" || raw.equals("true", ignoreCase = true)
    }

    fun getInt(key: String, fallback: Int): Int {
        val raw = _state.value[key] ?: return fallback
        return raw.toIntOrNull() ?: fallback
    }

    fun getJsonObject(key: String): JsonObject? {
        val raw = _state.value[key] ?: return null
        return runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
    }

    fun set(key: String, value: String) {
        _state.update { it + (key to value) }
        scope.launch {
            runCatching { ApiClient.api.putState(StatePutBody(key = key, value = value)) }
        }
    }

    fun setJson(key: String, value: JsonElement) {
        set(key, value.toString())
    }

    fun setBool(key: String, value: Boolean) = set(key, if (value) "1" else "0")

    fun pageSize(): Int {
        val value = getInt("mw/pageSize", 30)
        return if (value in listOf(10, 20, 30, 50, 100)) value else 30
    }

    /* ------------------------------------------------------------ helpers */

    suspend fun playlistPaths(): List<String> = ApiClient.api.playlist().paths

    suspend fun addToPlaylist(paths: List<String>): Int =
        ApiClient.api.playlistAdd(PathsBody(paths)).added

    suspend fun removeFromPlaylist(paths: List<String>): Int =
        ApiClient.api.playlistRemove(PathsBody(paths)).removed

    suspend fun config(): AppConfig = ApiClient.api.config()
}
