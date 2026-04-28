package co.loopr.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the long-lived device token + screen identity returned by /devices/poll.
 * Once we have these the player is "paired" and skips the pairing screen on relaunch.
 */
class DeviceStore(private val ds: DataStore<Preferences>) {
    private companion object {
        val TOKEN        = stringPreferencesKey("device_token")
        val SCREEN_ID    = longPreferencesKey("screen_id")
        val WORKSPACE_ID = longPreferencesKey("workspace_id")
        val SCREEN_NAME  = stringPreferencesKey("screen_name")
    }

    data class Identity(
        val deviceToken: String,
        val screenId: Long,
        val workspaceId: Long,
        val screenName: String,
    )

    val identity: Flow<Identity?> = ds.data.map { prefs ->
        val t = prefs[TOKEN] ?: return@map null
        val s = prefs[SCREEN_ID] ?: return@map null
        val w = prefs[WORKSPACE_ID] ?: return@map null
        val n = prefs[SCREEN_NAME] ?: return@map null
        Identity(t, s, w, n)
    }

    suspend fun store(token: String, screenId: Long, workspaceId: Long, screenName: String) {
        ds.edit { prefs ->
            prefs[TOKEN]        = token
            prefs[SCREEN_ID]    = screenId
            prefs[WORKSPACE_ID] = workspaceId
            prefs[SCREEN_NAME]  = screenName
        }
    }

    suspend fun clear() {
        ds.edit { it.clear() }
    }
}
