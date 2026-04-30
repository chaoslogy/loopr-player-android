package co.loopr.player.ui.player

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import co.loopr.player.api.AssignedPlaylistView
import co.loopr.player.api.ClockOverlay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import co.loopr.player.ui.theme.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun PlayerScreen(vm: PlayerViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is PlayerState.Loading       -> Loading()
            is PlayerState.Idle          -> Idle(s.screenName)
            is PlayerState.Playing       -> Playing(s)
            is PlayerState.Error         -> Idle("Loopr")  // graceful fallback
        }
        val clock = (state as? PlayerState.Playing)?.clock
            ?: (state as? PlayerState.Idle)?.clock
        if (clock != null && clock.enabled) ClockOverlayView(clock)
    }
}

@Composable
private fun Loading() {
    Box(
        Modifier
            .fillMaxSize()
            .background(LooprBlack),
        contentAlignment = Alignment.Center,
    ) {
        Text("Loopr", color = LooprTextDim, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Idle(screenName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(LooprPanelLight, LooprBlack))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓", color = LooprEmerald, fontSize = 88.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Connected", color = LooprText, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(screenName, color = LooprTextDim, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier
                    .background(LooprPanel, RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 18.dp)
            ) {
                Text(
                    "Assign a playlist in Loopr Studio to start playback.",
                    color = LooprTextDim, fontSize = 18.sp,
                )
            }
        }
        Text(
            "Loopr",
            modifier = Modifier.align(Alignment.BottomEnd).padding(40.dp),
            color = LooprTextMuted, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
        )
    }
}

@Composable
private fun Playing(state: PlayerState.Playing) {
    val item = state.items.getOrNull(state.cursor.coerceIn(0, state.items.size - 1)) ?: return
    val url = item.widget?.let { resolveWidgetUrl(it) }

    Box(Modifier.fillMaxSize().background(LooprBlack)) {
        if (url != null) {
            // Each Item is a different `key` so the WebView is recreated when
            // the cursor advances — no stale state from the previous slide.
            WebViewSlide(url = url, key = "${state.playlistName}#${item.position}")
        } else {
            Idle(state.screenName)
        }

        // Tiny status pill in the corner — useful while developing
        Text(
            "${state.cursor + 1} / ${state.items.size} · ${state.playlistName}",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(LooprPanel, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            color = LooprTextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewSlide(url: String, key: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                setBackgroundColor(0xFF000000.toInt())
                tag = key
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.tag != key) {
                webView.tag = key
                webView.loadUrl(url)
            }
        },
    )
}

private val widgetJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun resolveWidgetUrl(widget: AssignedPlaylistView.Playlist.Widget): String? = runCatching {
    val cfg = widgetJson.parseToJsonElement(widget.configJson).jsonObject
    when (widget.kind) {
        "web_url"  -> cfg["url"]?.jsonPrimitive?.content
        "youtube"  -> {
            val playlistId = cfg["playlistId"]?.jsonPrimitive?.content
            val videoId    = cfg["videoId"]?.jsonPrimitive?.content
            val mute       = (cfg["mute"]?.jsonPrimitive?.content ?: "true") == "true"
            val muteParam  = if (mute) "&mute=1" else ""
            when {
                playlistId != null ->
                    "https://www.youtube.com/embed/videoseries?list=$playlistId&autoplay=1&loop=1$muteParam&controls=0&modestbranding=1"
                videoId != null ->
                    "https://www.youtube.com/embed/$videoId?autoplay=1&loop=1$muteParam&controls=0&modestbranding=1&playlist=$videoId"
                else -> null
            }
        }
        "canva" -> cfg["embedUrl"]?.jsonPrimitive?.content ?: cfg["url"]?.jsonPrimitive?.content
        else -> null
    }
}.getOrNull()


@Composable
private fun ClockOverlayView(clock: ClockOverlay) {
    var nowText by remember { mutableStateOf(formatNow(clock.format)) }
    LaunchedEffect(clock.format) {
        while (true) {
            nowText = formatNow(clock.format)
            // align to next second
            val now = System.currentTimeMillis()
            val msToNextSecond = 1000L - (now % 1000L)
            delay(msToNextSecond)
        }
    }
    val align = when (clock.position) {
        "top-left"     -> Alignment.TopStart
        "top-right"    -> Alignment.TopEnd
        "bottom-left"  -> Alignment.BottomStart
        else           -> Alignment.BottomEnd
    }
    val (bg, fg) = if (clock.theme == "dark")
        Color(0xFF111111) to Color(0xFFFFFFFF)
    else
        Color(0xFFFFFFFF) to Color(0xFF0F1115)
    Box(
        Modifier.fillMaxSize().alpha(clock.opacity.coerceIn(0f, 1f)),
        contentAlignment = align,
    ) {
        Box(
            Modifier
                .padding(40.dp)
                .background(bg, RoundedCornerShape(20.dp))
                .padding(horizontal = 28.dp, vertical = 16.dp),
        ) {
            Text(
                nowText,
                color = fg,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
            )
        }
    }
}

private fun formatNow(format: String): String {
    val pattern = if (format == "24h") "HH:mm" else "h:mma"
    return LocalTime.now().format(DateTimeFormatter.ofPattern(pattern))
        .lowercase() // 3:43am style like juuno
}
