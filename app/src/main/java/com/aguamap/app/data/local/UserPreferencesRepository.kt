package com.aguamap.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aguamap.app.domain.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_SECTOR: Preferences.Key<String> = stringPreferencesKey("selected_sector")
        val IS_HIGH_CONTRAST: Preferences.Key<Boolean> = booleanPreferencesKey("is_high_contrast")
        val SEARCH_RADIUS: Preferences.Key<Float> = floatPreferencesKey("search_radius")
        val IS_ANONYMOUS: Preferences.Key<Boolean> = booleanPreferencesKey("is_anonymous")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                selectedSector = preferences[PreferencesKeys.SELECTED_SECTOR] ?: "Todos",
                isHighContrast = preferences[PreferencesKeys.IS_HIGH_CONTRAST] ?: false,
                searchRadius = preferences[PreferencesKeys.SEARCH_RADIUS] ?: 1.0f,
                isAnonymous = preferences[PreferencesKeys.IS_ANONYMOUS] ?: false
            )
        }

    suspend fun updateSector(sector: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_SECTOR] = sector
        }
    }

    suspend fun updateHighContrast(isHighContrast: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_HIGH_CONTRAST] = isHighContrast
        }
    }

    suspend fun updateRadius(radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_RADIUS] = radius
        }
    }

    suspend fun updateAnonymous(isAnonymous: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ANONYMOUS] = isAnonymous
        }
    }
}
