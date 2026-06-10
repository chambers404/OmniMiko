package com.omnimiko.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** User-facing preferences backed by Preferences DataStore. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        AppSettings(
            activeModelId = p[KEY_ACTIVE_MODEL],
            temperature = p[KEY_TEMPERATURE] ?: 0.7f,
            maxIterations = p[KEY_MAX_ITERS] ?: 8,
            autoApproveTools = p[KEY_AUTO_APPROVE] ?: false,
            persona = p[KEY_PERSONA],
        )
    }

    suspend fun setActiveModel(id: String?) = dataStore.edit { p ->
        if (id == null) p.remove(KEY_ACTIVE_MODEL) else p[KEY_ACTIVE_MODEL] = id
    }

    suspend fun setTemperature(value: Float) = dataStore.edit { it[KEY_TEMPERATURE] = value }

    suspend fun setMaxIterations(value: Int) = dataStore.edit { it[KEY_MAX_ITERS] = value }

    suspend fun setAutoApproveTools(value: Boolean) = dataStore.edit { it[KEY_AUTO_APPROVE] = value }

    suspend fun setPersona(value: String?) = dataStore.edit { p ->
        if (value.isNullOrBlank()) p.remove(KEY_PERSONA) else p[KEY_PERSONA] = value
    }

    private companion object {
        val KEY_ACTIVE_MODEL = stringPreferencesKey("active_model_id")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_MAX_ITERS = intPreferencesKey("max_iterations")
        val KEY_AUTO_APPROVE = booleanPreferencesKey("auto_approve_tools")
        val KEY_PERSONA = stringPreferencesKey("persona")
    }
}

data class AppSettings(
    val activeModelId: String? = null,
    val temperature: Float = 0.7f,
    val maxIterations: Int = 8,
    val autoApproveTools: Boolean = false,
    val persona: String? = null,
)
