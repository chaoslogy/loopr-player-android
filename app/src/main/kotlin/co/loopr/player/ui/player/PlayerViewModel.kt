package co.loopr.player.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.loopr.player.LooprApp
import co.loopr.player.api.AssignedPlaylistView
import co.loopr.player.api.ClockOverlay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import co.loopr.player.data.DeviceStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PlayerState {
    data object Loading : PlayerState
    data class Idle(val screenName: String, val clock: ClockOverlay? = null) : PlayerState
    data class Playing(
        val screenName: String,
        val playlistName: String,
        val items: List<AssignedPlaylistView.Playlist.Item>,
        val cursor: Int,
        val clock: ClockOverlay? = null,
        val deviceToken: String? = null,
    ) : PlayerState
    data class Error(val message: String) : PlayerState
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val looprApp = app as LooprApp
    private val _state = MutableStateFlow<PlayerState>(PlayerState.Loading)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val identityFlow = looprApp.deviceStore.identity

    private val overlaysJson = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun extractClock(overlays: JsonElement?): ClockOverlay? {
        if (overlays == null) return null
        return runCatching {
            val obj = overlays as? kotlinx.serialization.json.JsonObject ?: return null
            val clockNode = obj["clock"] ?: return null
            overlaysJson.decodeFromJsonElement(ClockOverlay.serializer(), clockNode)
                .takeIf { it.enabled }
        }.getOrNull()
    }

    init {
        viewModelScope.launch { runFetchLoop() }
        viewModelScope.launch { runCursorLoop() }
    }

    /** Refresh the assigned playlist every 30 seconds. */
    private suspend fun runFetchLoop() {
        while (true) {
            val identity: DeviceStore.Identity = identityFlow.first { it != null }!!
            val result = looprApp.api.fetchAssignedPlaylist(identity.deviceToken)
            result.onSuccess { view ->
                val clock = extractClock(view.screen.overlays)
                if (view.playlist == null || view.playlist.items.isEmpty()) {
                    _state.update { PlayerState.Idle(identity.screenName, clock) }
                } else {
                    val current = _state.value
                    val keepCursor = current is PlayerState.Playing
                            && current.playlistName == view.playlist.name
                            && current.items.size == view.playlist.items.size
                    val cursor = if (keepCursor) (current as PlayerState.Playing).cursor else 0
                    _state.update {
                        PlayerState.Playing(
                            screenName = identity.screenName,
                            playlistName = view.playlist.name,
                            items = view.playlist.items,
                            cursor = cursor,
                            clock = clock,
                            deviceToken = identity.deviceToken,
                        )
                    }
                }
            }.onFailure { e ->
                _state.update { PlayerState.Error(e.message ?: "couldn't fetch playlist") }
            }
            delay(30_000)
        }
    }

    /** Tick the slide cursor based on the active item's durationSeconds. */
    private suspend fun runCursorLoop() {
        while (true) {
            val current = _state.value
            if (current is PlayerState.Playing && current.items.isNotEmpty()) {
                val item = current.items[current.cursor.coerceIn(0, current.items.size - 1)]
                delay(item.durationSeconds.coerceAtLeast(2) * 1000L)
                _state.update {
                    if (it is PlayerState.Playing && it.items.isNotEmpty()) {
                        it.copy(cursor = (it.cursor + 1) % it.items.size)
                    } else it
                }
            } else {
                delay(1000)
            }
        }
    }
}
