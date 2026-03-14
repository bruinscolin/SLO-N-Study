package dev.csse.cbjl.slo_n_study

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Creates a single DataStore instance tied to the app context
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesRepository(private val context: Context) {

    companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
    }

    // Emits the current list of favorites whenever it changes
    val favoritesFlow: Flow<List<StudySpot>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITES_KEY]
            ?.mapNotNull { json ->
                runCatching { Json.decodeFromString<StudySpot>(json) }.getOrNull()
            }
            ?: emptyList()
    }

    suspend fun saveFavorites(spots: List<StudySpot>) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITES_KEY] = spots.map { Json.encodeToString(it) }.toSet()
        }
    }
}