package co.loopr.player.ui.pairing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.loopr.player.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PairingScreen(
    onClaimed: () -> Unit,
    vm: PairingViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is PairingState.Claimed) {
            // Brief celebratory pause then navigate
            delay(1200)
            onClaimed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(LooprPanelLight, LooprBlack),
                    radiusFraction = 1f
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (val s = state) {
            is PairingState.RequestingCode -> RequestingCode()
            is PairingState.ShowingCode    -> ShowingCode(code = s.code, pairUrl = s.pairUrl)
            is PairingState.NetworkError   -> NetworkError(message = s.message, onRetry = vm::retry)
            is PairingState.Claimed        -> Claimed()
        }

        // Bottom-right wordmark
        Text(
            "Loopr",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(40.dp),
            color = LooprTextMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun RequestingCode() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = LooprRoyalHi, strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text("Connecting to Loopr…", color = LooprTextDim, fontSize = 18.sp)
    }
}

@Composable
private fun ShowingCode(code: String, pairUrl: String) {
    val parts = if (code.contains('-')) code.split('-') else listOf(code.take(3), code.drop(3))
    val left = parts.getOrNull(0).orEmpty()
    val right = parts.getOrNull(1).orEmpty()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Pair this TV",
            color = LooprTextMuted,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            letterSpacing = 4.sp,
        )
        Spacer(Modifier.height(36.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            CodeGroup(text = left)
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(20.dp)
                    .height(8.dp)
                    .background(LooprTextMuted, RoundedCornerShape(4.dp))
            )
            CodeGroup(text = right)
        }

        Spacer(Modifier.height(48.dp))
        Text(
            "Visit",
            color = LooprTextMuted,
            fontSize = 18.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            pairUrl,
            color = LooprText,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(36.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BlinkingDot()
            Spacer(Modifier.width(10.dp))
            Text(
                "Waiting for someone to enter the code…",
                color = LooprTextMuted,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun CodeGroup(text: String) {
    Row {
        text.forEach { ch ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(width = 96.dp, height = 128.dp)
                    .border(2.dp, LooprBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    ch.toString(),
                    color = LooprText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 88.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun BlinkingDot() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) { delay(700); visible = !visible }
    }
    AnimatedVisibility(visible) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(LooprEmerald, shape = RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun NetworkError(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚠", color = LooprWarning, fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Couldn't reach Loopr",
            color = LooprText,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(message, color = LooprTextMuted, fontSize = 16.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Retrying in 5 seconds…",
            color = LooprTextDim,
            fontSize = 14.sp,
        )
    }
    LaunchedEffect(Unit) {
        delay(5000)
        onRetry()
    }
}

@Composable
private fun Claimed() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✓", color = LooprEmerald, fontSize = 96.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Connected",
            color = LooprText,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
        )
    }
}

// Keep MaterialTheme reference so unused-import lint stays happy
@Suppress("unused")
private fun touch() = MaterialTheme.colorScheme
