package fr.jc14fr.mutualisons

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object ServerSettings {
    private val KEY_URL = stringPreferencesKey("server_url")
    private val KEY_TOKEN = stringPreferencesKey("auth_token")

    fun serverUrl(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_URL] }

    suspend fun saveServerUrl(context: Context, url: String) {
        context.dataStore.edit { it[KEY_URL] = url.trimEnd('/') }
    }

    fun token(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_TOKEN] }

    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun clearToken(context: Context) {
        context.dataStore.edit { it.remove(KEY_TOKEN) }
    }
}

fun isValidServerUrl(url: String): Boolean {
    val u = url.trim()
    return (u.startsWith("https://") || u.startsWith("http://")) && u.length > 10 && !u.contains(" ")
}