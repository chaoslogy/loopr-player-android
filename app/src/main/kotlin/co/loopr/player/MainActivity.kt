package co.loopr.player

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import co.loopr.player.ui.player.SettingsOverlay
import kotlinx.coroutines.launch
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import co.loopr.player.data.DeviceStore
import co.loopr.player.ui.pairing.PairingScreen
import co.loopr.player.ui.player.PlayerScreen
import co.loopr.player.ui.theme.LooprTheme

class MainActivity : ComponentActivity() {
    /** Set by the composition while a paired session is showing; the ☰ MENU
     *  button on the remote opens the settings sheet. */
    var onMenuKey: (() -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU) {
            onMenuKey?.let { it(); return true }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installImmersiveFullscreen()

        // Keep screen on for the duration of this activity
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            LooprTheme {
                Root(deviceStore = (application as LooprApp).deviceStore)
            }
        }
    }

    private fun installImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
private fun Root(deviceStore: DeviceStore) {
    val identity by deviceStore.identity.collectAsState(initial = null)
    var localOverride by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    val paired = identity != null && !localOverride

    // Wire the ☰ MENU remote button to the settings sheet only while paired.
    DisposableEffect(paired) {
        val act = ctx as? MainActivity
        act?.onMenuKey = if (paired) ({ showSettings = true }) else null
        onDispose { act?.onMenuKey = null }
    }

    if (paired) {
        Box(Modifier.fillMaxSize()) {
            PlayerScreen()
            if (showSettings) {
                BackHandler(enabled = true) { showSettings = false }
                identity?.let { id ->
                    SettingsOverlay(
                        identity = id,
                        onResume = { showSettings = false },
                        onUnpair = {
                            showSettings = false
                            scope.launch { deviceStore.clear() }
                        },
                    )
                }
            }
        }
    } else {
        PairingScreen(onClaimed = { localOverride = false /* identity flow will flip us */ })
    }
}
