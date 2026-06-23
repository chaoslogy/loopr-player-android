package co.loopr.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.loopr.player.BuildConfig
import co.loopr.player.data.DeviceStore
import co.loopr.player.ui.theme.*

/**
 * Remote-reachable settings sheet (opened with the MENU button). Shows which
 * Loopr account/screen this device is paired to and lets the user sign out /
 * unpair so they can connect a different account - without reinstalling the APK.
 */
@Composable
fun SettingsOverlay(
    identity: DeviceStore.Identity,
    onResume: () -> Unit,
    onUnpair: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    Box(
        Modifier.fillMaxSize().background(Color(0xCC050614)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .background(LooprPanel, RoundedCornerShape(24.dp))
                .border(1.dp, LooprBorder, RoundedCornerShape(24.dp))
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Loopr player", color = LooprTextDim, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(identity.screenName, color = LooprText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Workspace #${identity.workspaceId}  -  Screen #${identity.screenId}",
                color = LooprTextMuted, fontSize = 15.sp,
            )
            Text("v${BuildConfig.VERSION_NAME}", color = LooprTextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(30.dp))

            TvButton(
                label = "Resume",
                primary = true,
                modifier = Modifier.focusRequester(firstFocus),
                onClick = onResume,
            )
            Spacer(Modifier.height(12.dp))
            TvButton(
                label = "Sign out / switch account",
                primary = false,
                onClick = onUnpair,
            )

            Spacer(Modifier.height(22.dp))
            Text(
                "Signing out unpairs this device and returns to the pairing screen, " +
                    "so you can connect a different Loopr account.",
                color = LooprTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TvButton(
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bg = when {
        primary && focused -> LooprRoyalHi
        primary            -> LooprRoyal
        focused            -> LooprPanelLight
        else               -> LooprPanel
    }
    val borderColor = if (focused) LooprRoyalHi else LooprBorder
    Box(
        modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .border(if (focused) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .focusable(interactionSource = interaction)
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp &&
                    (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)
                ) {
                    onClick(); true
                } else {
                    false
                }
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = LooprText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}
