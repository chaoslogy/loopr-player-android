package co.loopr.player.ui.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.loopr.player.BuildConfig
import co.loopr.player.LooprApp
import co.loopr.player.api.ClaimCodeRequest
import co.loopr.player.data.DeviceFingerprint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PairingState {
    data object RequestingCode : PairingState
    data class ShowingCode(val code: String, val pairUrl: String) : PairingState
    data class NetworkError(val message: String) : PairingState
    data object Claimed : PairingState
}

class PairingViewModel(app: Application) : AndroidViewModel(app) {
    private val app = app as LooprApp
    private val _state = MutableStateFlow<PairingState>(PairingState.RequestingCode)
    val state: StateFlow<PairingState> = _state.asStateFlow()

    init {
        startFlow()
    }

    fun retry() = startFlow()

    private fun startFlow() = viewModelScope.launch {
        _state.update { PairingState.RequestingCode }

        val req = ClaimCodeRequest(
            deviceFingerprint = DeviceFingerprint.get(app),
            hardwareModel = DeviceFingerprint.hardwareModel(),
            appVersion = BuildConfig.VERSION_NAME,
        )

        val result = app.api.requestClaimCode(req)
        val resp = result.getOrElse {
            _state.update { PairingState.NetworkError(it.message ?: "couldn't reach api.loopr.studio") }
            return@launch
        }

        _state.update { PairingState.ShowingCode(resp.code, resp.pairUrl) }

        // Strip the dash for the API path; keep the dashed format for display
        val rawCode = resp.code.replace("-", "")

        // Poll
        val intervalMs = (resp.pollIntervalSeconds.coerceAtLeast(2)) * 1000L
        while (true) {
            delay(intervalMs)
            val pollResult = app.api.pollClaim(rawCode)
            val poll = pollResult.getOrElse {
                // 404 = code expired/unknown → request a fresh one
                _state.update { PairingState.RequestingCode }
                startFlow()
                return@launch
            }
            if (poll != null) {
                app.deviceStore.store(
                    token = poll.deviceToken,
                    screenId = poll.screenId,
                    workspaceId = poll.workspaceId,
                    screenName = poll.screenName,
                )
                _state.update { PairingState.Claimed }
                return@launch
            }
            // null = 202, keep polling
        }
    }
}
