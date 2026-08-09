package com.pavanpej.kompass.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pavanpej.kompass.location.CoordinateFormat
import com.pavanpej.kompass.sensor.NorthMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kompass_settings")

/** Persists user preferences (haptics, default north mode, coordinate format) across app restarts via DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val DEFAULT_NORTH_MODE = stringPreferencesKey("default_north_mode")
        val COORDINATE_FORMAT = stringPreferencesKey("coordinate_format")
    }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAPTICS_ENABLED] ?: true
    }

    val defaultNorthMode: Flow<NorthMode> = context.dataStore.data.map { prefs ->
        val stored = prefs[Keys.DEFAULT_NORTH_MODE]
        stored?.let { runCatching { NorthMode.valueOf(it) }.getOrNull() } ?: NorthMode.MAGNETIC
    }

    val coordinateFormat: Flow<CoordinateFormat> = context.dataStore.data.map { prefs ->
        val stored = prefs[Keys.COORDINATE_FORMAT]
        stored?.let { runCatching { CoordinateFormat.valueOf(it) }.getOrNull() } ?: CoordinateFormat.DECIMAL
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setDefaultNorthMode(mode: NorthMode) {
        context.dataStore.edit { it[Keys.DEFAULT_NORTH_MODE] = mode.name }
    }

    suspend fun setCoordinateFormat(format: CoordinateFormat) {
        context.dataStore.edit { it[Keys.COORDINATE_FORMAT] = format.name }
    }
}
