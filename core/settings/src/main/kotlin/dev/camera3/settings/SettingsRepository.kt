package dev.camera3.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.camera3.model.PhotoPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.cameraPreferences by preferencesDataStore("camera3")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.cameraPreferences
    private val grid = booleanPreferencesKey("grid")
    private val quality = intPreferencesKey("jpeg_quality")
    private val raw = booleanPreferencesKey("raw")
    private val highResolution = booleanPreferencesKey("high_resolution")
    val preferences = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map {
        PhotoPreferences(
            grid = it[grid] ?: false,
            jpegQuality = (it[quality] ?: 95).coerceIn(50, 100),
            raw = it[raw] ?: false,
            highResolution = it[highResolution] ?: false,
        )
    }

    suspend fun setGrid(enabled: Boolean) { store.edit { it[grid] = enabled } }
    suspend fun setRaw(enabled: Boolean) { store.edit { it[raw] = enabled } }
    suspend fun setHighResolution(enabled: Boolean) { store.edit { it[highResolution] = enabled } }
    suspend fun setPhotoModes(rawEnabled: Boolean, highResolutionEnabled: Boolean) {
        store.edit {
            it[raw] = rawEnabled
            it[highResolution] = highResolutionEnabled
        }
    }
    suspend fun setJpegQuality(value: Int) {
        require(value in 50..100)
        store.edit { it[quality] = value }
    }
}
