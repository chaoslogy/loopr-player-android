package co.loopr.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = LooprRoyalHi,
    onPrimary = LooprText,
    secondary = LooprSky,
    background = LooprBlack,
    surface = LooprPanel,
    onSurface = LooprText,
    onSurfaceVariant = LooprTextMuted,
    error = LooprWarning,
)

@Composable
fun LooprTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else DarkColors  // always dark for the player
    MaterialTheme(colorScheme = colors, content = content)
}
