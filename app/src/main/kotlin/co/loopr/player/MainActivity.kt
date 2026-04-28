package co.loopr.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import co.loopr.player.data.DeviceStore
import co.loopr.player.ui.pairing.PairingScreen
import co.loopr.player.ui.player.PlayerScreen
import co.loopr.player.ui.theme.LooprTheme

class MainActivity : ComponentActivity() {
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
    if (identity != null && !localOverride) {
        PlayerScreen()
    } else {
        PairingScreen(onClaimed = { localOverride = false /* identity flow will flip us */ })
    }
}
