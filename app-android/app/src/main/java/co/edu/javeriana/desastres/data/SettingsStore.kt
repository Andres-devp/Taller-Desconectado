package co.edu.javeriana.desastres.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import co.edu.javeriana.desastres.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "ajustes")

/** Preferencias del dispositivo: URL del sistema central e identificador del equipo. */
class SettingsStore(private val context: Context) {
    private val kUrl = stringPreferencesKey("server_url")
    private val kDispositivo = stringPreferencesKey("dispositivo_id")

    val serverUrl: Flow<String> =
        context.dataStore.data.map { it[kUrl] ?: BuildConfig.DEFAULT_SERVER_URL }

    suspend fun guardarServerUrl(url: String) {
        context.dataStore.edit { it[kUrl] = url.trim() }
    }

    suspend fun serverUrlActual(): String = serverUrl.first()

    /** Identificador estable del dispositivo, generado en el primer uso. */
    suspend fun dispositivoId(): String {
        context.dataStore.data.first()[kDispositivo]?.let { return it }
        val nuevo = "android-" + UUID.randomUUID().toString().take(8)
        context.dataStore.edit { prefs -> if (prefs[kDispositivo] == null) prefs[kDispositivo] = nuevo }
        return context.dataStore.data.first()[kDispositivo]!!
    }
}
