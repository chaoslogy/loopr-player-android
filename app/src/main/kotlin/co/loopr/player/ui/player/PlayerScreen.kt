package co.loopr.player.ui.player

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import co.loopr.player.LooprApp
import co.loopr.player.api.AssignedPlaylistView
import co.loopr.player.api.UrlSessionCredentials
import co.loopr.player.api.ClockOverlay
import co.loopr.player.api.TickerOverlay
import co.loopr.player.api.WeatherOverlay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import co.loopr.player.ui.theme.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun PlayerScreen(vm: PlayerViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val weatherNow by vm.weather.collectAsState()
    val orientation = (state as? PlayerState.Playing)?.orientation
        ?: (state as? PlayerState.Idle)?.orientation
    RotatedRoot(orientation) {
        Box(Modifier.fillMaxSize()) {
            when (val s = state) {
                is PlayerState.Loading       -> Loading()
                is PlayerState.Idle          -> Idle(s.screenName, s.branding)
                is PlayerState.Playing       -> Playing(s, onMediaEnded = { vm.advanceCursor() })
                is PlayerState.Error         -> Idle("Loopr")  // graceful fallback
            }
            val clock = (state as? PlayerState.Playing)?.clock
                ?: (state as? PlayerState.Idle)?.clock
            val weather = (state as? PlayerState.Playing)?.weather
                ?: (state as? PlayerState.Idle)?.weather
            val ticker = (state as? PlayerState.Playing)?.ticker
                ?: (state as? PlayerState.Idle)?.ticker
            CornerOverlays(clock, weather, weatherNow)
            if (ticker != null && ticker.enabled && ticker.text.isNotBlank()) TickerOverlayView(ticker)
        }
    }
}

/* --- orientation: rotate the whole content tree for portrait mounts ------- */

/**
 * Fire TV panels are physically landscape (e.g. 1920x1080); portrait signage is
 * the same panel mounted rotated 90 degrees. We don't rotate the activity —
 * we compose the UI at the swapped size (1080x1920 via requiredSize), centred
 * on the landscape surface, then spin it around its centre with graphicsLayer.
 * A WxH rect rotated 90/270 degrees about its centre occupies HxW, so the
 * rotated composition maps exactly onto the physical screen. Overlays live
 * inside the rotated root, so they inherit the rotation for free.
 */
@Composable
private fun RotatedRoot(orientation: String?, content: @Composable () -> Unit) {
    val angle = when (orientation) {
        "portrait"         -> 90f
        "portrait_flipped" -> 270f
        else               -> 0f   // "landscape", null, or anything unknown
    }
    if (angle == 0f) {
        Box(Modifier.fillMaxSize()) { content() }
    } else {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenW = maxWidth
            val screenH = maxHeight
            Box(
                Modifier
                    .align(Alignment.Center)
                    .requiredSize(width = screenH, height = screenW)
                    .graphicsLayer { rotationZ = angle },
            ) { content() }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize().background(LooprBlack), contentAlignment = Alignment.Center) {
        Text("Loopr", color = LooprTextDim, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Idle(
    screenName: String,
    branding: AssignedPlaylistView.ScreenIdentity.Branding? = null,
) {
    val accent = parseHexColor(branding?.accentColor)
    val logoUrl = branding?.logoUrl
    val brandName = branding?.brandName
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(LooprPanelLight, LooprBlack))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                !logoUrl.isNullOrBlank() -> {
                    coil.compose.AsyncImage(
                        model = logoUrl,
                        contentDescription = brandName ?: "logo",
                        modifier = Modifier.heightIn(max = 160.dp).widthIn(max = 360.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                }
                !brandName.isNullOrBlank() -> {
                    Text(brandName, color = accent ?: LooprText, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                }
                else -> {
                    Text("✓", color = accent ?: LooprEmerald, fontSize = 88.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                }
            }
            Text("Connected", color = LooprText, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(screenName, color = LooprTextDim, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(28.dp))
            if (accent != null) {
                Box(Modifier.height(4.dp).width(72.dp).background(accent, RoundedCornerShape(2.dp)))
                Spacer(Modifier.height(28.dp))
            } else {
                Spacer(Modifier.height(12.dp))
            }
            Box(Modifier.background(LooprPanel, RoundedCornerShape(20.dp)).padding(28.dp, 18.dp)) {
                Text("Assign a playlist in Loopr Studio to start playback.",
                    color = LooprTextDim, fontSize = 18.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("Press the ☰ MENU button on the remote to switch account.",
                color = LooprTextMuted, fontSize = 14.sp)
        }
        Text(brandName?.takeIf { it.isNotBlank() } ?: "Loopr",
            modifier = Modifier.align(Alignment.BottomEnd).padding(40.dp),
            color = LooprTextMuted, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

/** Parse "#RRGGBB" or "#RRGGBBAA" into a Compose Color; null if absent/invalid. */
private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val h = hex.trim().removePrefix("#")
        when (h.length) {
            6 -> Color(android.graphics.Color.parseColor("#$h"))
            8 -> {
                // input is #RRGGBBAA; android wants #AARRGGBB
                val rr = h.substring(0, 2); val gg = h.substring(2, 4)
                val bb = h.substring(4, 6); val aa = h.substring(6, 8)
                Color(android.graphics.Color.parseColor("#$aa$rr$gg$bb"))
            }
            else -> null
        }
    } catch (e: Exception) { null }
}

@Composable
private fun Playing(state: PlayerState.Playing, onMediaEnded: () -> Unit) {
    val item = state.items.getOrNull(state.cursor.coerceIn(0, state.items.size - 1)) ?: return

    Box(Modifier.fillMaxSize().background(LooprBlack)) {
        when (item.kind) {
            "media" -> {
                val media = item.media
                if (media != null) {
                    MediaSlot(media, key = "${state.playlistName}#${item.position}", onEnded = onMediaEnded)
                } else {
                    Idle(state.screenName)
                }
            }
            "widget" -> {
                val widget = item.widget
                when {
                    widget == null -> Idle(state.screenName)
                    widget.kind == "split" -> {
                        val split = resolveSplit(widget)
                        if (split != null) {
                            SplitSlot(
                                split = split,
                                keyBase = "${state.playlistName}#${item.position}",
                                deviceToken = state.deviceToken,
                            )
                        } else {
                            Idle(state.screenName)
                        }
                    }
                    else -> {
                        val resolved = resolveWidget(widget)
                        if (resolved != null) {
                            WebSlot(
                                resolved = resolved,
                                key = "${state.playlistName}#${item.position}",
                                deviceToken = state.deviceToken,
                            )
                        } else {
                            Idle(state.screenName)
                        }
                    }
                }
            }
            else -> Idle(state.screenName)
        }
    }
}

/* --- media slot: image (Coil) or video (ExoPlayer) ------------------------ */

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun MediaSlot(
    media: AssignedPlaylistView.Playlist.Media,
    key: String,
    onEnded: () -> Unit,
) {
    when (media.kind) {
        "image", "gif" -> {
            coil.compose.AsyncImage(
                model = media.publicUrl,
                contentDescription = media.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize().background(LooprBlack),
            )
        }
        "video" -> {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            // Capture the latest onEnded so listener never fires a stale closure.
            val latestOnEnded by androidx.compose.runtime.rememberUpdatedState(onEnded)
            val player = remember(key) {
                androidx.media3.exoplayer.ExoPlayer.Builder(ctx).build().apply {
                    setMediaItem(androidx.media3.common.MediaItem.fromUri(media.publicUrl))
                    // Single playthrough — playlist cursor advances on STATE_ENDED below.
                    // Falling back to OFF (not REPEAT_MODE_ONE) so a long video doesn't loop while we wait.
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
                    playWhenReady = true
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == androidx.media3.common.Player.STATE_ENDED) {
                                latestOnEnded()
                            }
                        }
                    })
                    prepare()
                }
            }
            androidx.compose.runtime.DisposableEffect(key) {
                onDispose { player.release() }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    androidx.media3.ui.PlayerView(c).apply {
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        this.player = player
                    }
                },
                update = { it.player = player },
            )
        }
        else -> Idle("")
    }
}

/* --- web slot: WebView with optional auth + refresh + countdown ----------- */

private data class ResolvedWidget(
    val url: String,
    val refreshSeconds: Int,    // 0 = no refresh
    val urlSessionId: Long?,    // null = public URL
    val embedHtml: String? = null,  // when set, load this HTML (with EMBED_ORIGIN base) instead of url
)

/* A stable https origin used as the WebView base URL when we host a third-party
 * embed in a wrapper page. YouTube's /embed/ endpoint throws a configuration
 * error (e.g. "Error 153") when loaded as a *top-level* document with no parent
 * origin/Referer — so YouTube widgets are loaded inside this iframe host page
 * instead, which gives the player iframe a valid origin to validate against. */
private const val EMBED_ORIGIN = "https://app.loopr.studio"

private fun youtubeHostHtml(embedUrl: String): String {
    val src = "$embedUrl&origin=$EMBED_ORIGIN"
    return """<!doctype html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style></head>
<body><iframe src="$src" allow="autoplay; encrypted-media; fullscreen" allowfullscreen></iframe></body></html>"""
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebSlot(resolved: ResolvedWidget, key: String, deviceToken: String?, fitToWidth: Boolean = false) {
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as LooprApp)
    var creds by remember(key) { mutableStateOf<UrlSessionCredentials?>(null) }
    var loginRequired by remember(key) { mutableStateOf(false) }
    var lastLoadedAt by remember(key) { mutableStateOf(System.currentTimeMillis()) }
    var nowMs by remember(key) { mutableLongStateOf(System.currentTimeMillis()) }
    var reloadToken by remember(key) { mutableStateOf(0) }

    // Fetch credentials once per item if a session is configured
    LaunchedEffect(key, resolved.urlSessionId, deviceToken) {
        if (resolved.urlSessionId != null && deviceToken != null) {
            val r = app.api.fetchUrlSessionCredentials(deviceToken, resolved.urlSessionId)
            r.onSuccess { c ->
                if (c == null) loginRequired = true else { creds = c; loginRequired = false }
            }.onFailure { loginRequired = true }
        }
    }

    // Tick "now" every second so the countdown updates
    LaunchedEffect(key) {
        while (true) { nowMs = System.currentTimeMillis(); delay(1000) }
    }

    if (loginRequired) {
        LoginRequiredPlaceholder(resolved.url)
        return
    }

    Box(Modifier.fillMaxSize()) {
        key(reloadToken) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // Force desktop layout: sites sniff the UA + viewport. Pretending to
                        // be desktop Chrome on Linux makes them serve the full layout (which
                        // is also what we want on a 10-foot TV).
                        userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                        // Wide viewport on so loadWithOverviewMode actually shrinks-to-fit.
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    // Split panels are a fraction of the screen, so let overview mode
                    // shrink the full 1600px layout to fit the panel (scale 0 = default/
                    // overview). Full-screen keeps an 85% zoom for denser desktop content.
                    setInitialScale(if (fitToWidth) 0 else 85)
                    // Split panels must NOT pin initial-scale=1 (that defeats shrink-to-fit
                    // and crops the panel); full-screen keeps it for a 1:1 desktop look.
                    val viewportContent = if (fitToWidth) "width=1600" else "width=1600,initial-scale=1"
                    // *** Critical *** without these, every URL navigation gets delegated
                    // to the system browser. On Fire TV that's Silk, not us.
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            // Inject (or upgrade) a viewport meta so the page lays out
                            // at 1600 CSS px regardless of the WebView's pixel width.
                            // The WebView will scale the 1600-px layout down to fit the
                            // actual panel — which keeps the desktop chrome that
                            // testers see on their laptops.
                            view.evaluateJavascript(
                                "(function(){var c='" + viewportContent + "';var m=document.querySelector('meta[name=\"viewport\"]');if(m){m.setAttribute('content',c);}else{var n=document.createElement('meta');n.setAttribute('name','viewport');n.setAttribute('content',c);(document.head||document.documentElement).appendChild(n);}})();",
                                null,
                            )
                        }
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            // Re-apply after the page's own JS runs (some SPAs replace
                            // the viewport meta on hydrate).
                            view.evaluateJavascript(
                                "(function(){var c='" + viewportContent + "';var m=document.querySelector('meta[name=\"viewport\"]');if(m){m.setAttribute('content',c);}else{var n=document.createElement('meta');n.setAttribute('name','viewport');n.setAttribute('content',c);(document.head||document.documentElement).appendChild(n);}})();",
                                null,
                            )
                        }
                        override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                            // Fire TV killed this panel's WebView renderer (OOM after a long run,
                            // common with 4 panels going for hours). Recreate the panel instead
                            // of leaving it black.
                            android.os.Handler(android.os.Looper.getMainLooper()).post { reloadToken++ }
                            return true
                        }
                    }
                    webChromeClient = WebChromeClient()
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    setBackgroundColor(0xFF000000.toInt())
                    tag = "init"
                }
            },
            update = { wv ->
                val expectedTag = "$key:${creds?.sessionId ?: "none"}:$lastLoadedAt"
                if (wv.tag != expectedTag) {
                    wv.tag = expectedTag
                    creds?.let { applyCookies(it) }
                    val html = resolved.embedHtml
                    if (html != null) wv.loadDataWithBaseURL(EMBED_ORIGIN, html, "text/html", "utf-8", null)
                    else wv.loadUrl(resolved.url)
                }
            },
        )
        }

        // Auto-refresh tick
        if (resolved.refreshSeconds > 0) {
            LaunchedEffect(key, resolved.refreshSeconds, lastLoadedAt) {
                delay(resolved.refreshSeconds * 1000L)
                lastLoadedAt = System.currentTimeMillis()
            }
            // (CountdownPill removed — dev affordance)
        }
    }
}

@Composable
private fun CountdownPill(refreshSeconds: Int, lastLoadedAt: Long, nowMs: Long) {
    val secondsLeft = ((lastLoadedAt + refreshSeconds * 1000L - nowMs) / 1000L)
        .coerceAtLeast(0L)
    val mm = secondsLeft / 60
    val ss = secondsLeft % 60
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            Modifier
                .background(LooprPanel, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                "↻ %02d:%02d".format(mm, ss),
                color = LooprTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun LoginRequiredPlaceholder(url: String) {
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF38231A), LooprBlack))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔐", fontSize = 80.sp)
            Spacer(Modifier.height(20.dp))
            Text("Login required", color = LooprText, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(url, color = LooprTextDim, fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.background(LooprPanel, RoundedCornerShape(20.dp)).padding(28.dp, 16.dp)) {
                Text("Re-authenticate this URL session in Loopr Studio.",
                    color = LooprTextMuted, fontSize = 16.sp)
            }
        }
    }
}

private fun applyCookies(creds: UrlSessionCredentials) {
    val cm = CookieManager.getInstance()
    cm.setAcceptCookie(true)
    creds.cookies.forEach { c ->
        val sb = StringBuilder()
        sb.append(c.name).append('=').append(c.value)
        c.domain?.let { sb.append("; Domain=").append(it) }
        sb.append("; Path=").append(c.path)
        c.expires?.let { sb.append("; Expires=").append(java.util.Date(it * 1000).toGMTString()) }
        if (c.secure) sb.append("; Secure")
        if (c.httpOnly) sb.append("; HttpOnly")
        if (c.sameSite.isNotBlank()) sb.append("; SameSite=").append(c.sameSite)
        // CookieManager wants the URL prefix; build "https://<domain><path>"
        val host = (c.domain ?: "").trimStart('.')
        val urlForCookie = "https://$host${c.path}"
        cm.setCookie(urlForCookie, sb.toString())
    }
    cm.flush()
}

private val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT

private val widgetJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun resolveWidget(widget: AssignedPlaylistView.Playlist.Widget): ResolvedWidget? = runCatching {
    val cfg: JsonObject = widgetJson.parseToJsonElement(widget.configJson).jsonObject
    var embedHtml: String? = null
    val url: String? = when (widget.kind) {
        "web_url" -> cfg["url"]?.jsonPrimitive?.content
        "youtube" -> {
            val playlistId = cfg["playlistId"]?.jsonPrimitive?.content
            val videoId    = cfg["videoId"]?.jsonPrimitive?.content
            val mute       = (cfg["mute"]?.jsonPrimitive?.content ?: "true") == "true"
            val muteParam  = if (mute) "&mute=1" else ""
            val embedUrl = when {
                playlistId != null ->
                    "https://www.youtube.com/embed/videoseries?list=$playlistId&autoplay=1&loop=1$muteParam&controls=0&modestbranding=1&playsinline=1&rel=0"
                videoId != null ->
                    "https://www.youtube.com/embed/$videoId?autoplay=1&loop=1$muteParam&controls=0&modestbranding=1&playlist=$videoId&playsinline=1&rel=0"
                else -> null
            }
            embedUrl?.let { embedHtml = youtubeHostHtml(it) }
            embedUrl
        }
        "canva" -> cfg["embedUrl"]?.jsonPrimitive?.content ?: cfg["url"]?.jsonPrimitive?.content
        else    -> null
    }
    url ?: return null
    val refresh = cfg["refreshSeconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
    val urlSessionId = cfg["urlSessionId"]?.jsonPrimitive?.content?.toLongOrNull()
    ResolvedWidget(url, refresh, urlSessionId, embedHtml)
}.getOrNull()



/* --- split slot: 2-up or 2x2 grid of WebSlots --------------------------- */

private enum class SplitLayout { TwoUp, TwoByTwo }

private data class ResolvedSplit(
    val layout: SplitLayout,
    val panels: List<ResolvedWidget>,   // exactly 2 for TwoUp, exactly 4 for TwoByTwo
)

private fun resolveSplit(widget: AssignedPlaylistView.Playlist.Widget): ResolvedSplit? = runCatching {
    val cfg: JsonObject = widgetJson.parseToJsonElement(widget.configJson).jsonObject
    val layout = when (cfg["layout"]?.jsonPrimitive?.content) {
        "2x2" -> SplitLayout.TwoByTwo
        else  -> SplitLayout.TwoUp     // default = side-by-side
    }
    val want = if (layout == SplitLayout.TwoByTwo) 4 else 2
    val panelArr = cfg["panels"]?.let { it as? kotlinx.serialization.json.JsonArray } ?: return null
    val panels = panelArr.mapNotNull { e ->
        val o = (e as? JsonObject) ?: return@mapNotNull null
        val url = o["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val refresh = o["refreshSeconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val sid = o["urlSessionId"]?.jsonPrimitive?.content?.toLongOrNull()
        ResolvedWidget(url, refresh, sid)
    }
    if (panels.size < want) return null
    ResolvedSplit(layout, panels.take(want))
}.getOrNull()

@Composable
private fun SplitSlot(split: ResolvedSplit, keyBase: String, deviceToken: String?) {
    when (split.layout) {
        SplitLayout.TwoUp -> {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxSize().background(LooprBlack),
            ) {
                split.panels.forEachIndexed { i, panel ->
                    Box(modifier = Modifier.fillMaxHeight().weight(1f)) {
                        WebSlot(
                            resolved = panel,
                            key = "$keyBase#p$i",
                            deviceToken = deviceToken,
                            fitToWidth = true,
                        )
                    }
                }
            }
        }
        SplitLayout.TwoByTwo -> {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize().background(LooprBlack),
            ) {
                listOf(0 to 1, 2 to 3).forEach { (l, r) ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        listOf(l, r).forEach { idx ->
                            Box(modifier = Modifier.fillMaxHeight().weight(1f)) {
                                WebSlot(
                                    resolved = split.panels[idx],
                                    key = "$keyBase#p$idx",
                                    deviceToken = deviceToken,
                                    fitToWidth = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* --- corner overlays: clock + weather chips ------------------------------- */

/** Shared light/dark chip palette — background to foreground. */
private fun overlayColors(theme: String): Pair<Color, Color> =
    if (theme == "dark")
        Color(0xFF111111) to Color(0xFFFFFFFF)
    else
        Color(0xFFFFFFFF) to Color(0xFF0F1115)

private fun normalizeCorner(position: String): String = when (position) {
    "top-left", "top-right", "bottom-left" -> position
    else -> "bottom-right"
}

private fun cornerAlignment(corner: String): Alignment = when (corner) {
    "top-left"     -> Alignment.TopStart
    "top-right"    -> Alignment.TopEnd
    "bottom-left"  -> Alignment.BottomStart
    else           -> Alignment.BottomEnd
}

/**
 * Renders the clock + weather chips at their configured corners. Chips that
 * share a corner stack vertically (clock above weather). The weather chip is
 * hidden until the first successful fetch lands.
 */
@Composable
private fun CornerOverlays(clock: ClockOverlay?, weather: WeatherOverlay?, weatherNow: WeatherNow?) {
    listOf("top-left", "top-right", "bottom-left", "bottom-right").forEach { corner ->
        val clockHere = clock?.takeIf { it.enabled && normalizeCorner(it.position) == corner }
        val weatherHere = weather?.takeIf {
            it.enabled && normalizeCorner(it.position) == corner && weatherNow != null
        }
        if (clockHere == null && weatherHere == null) return@forEach
        Box(Modifier.fillMaxSize(), contentAlignment = cornerAlignment(corner)) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = if (corner.endsWith("right")) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (clockHere != null) ClockChip(clockHere)
                if (weatherHere != null && weatherNow != null) WeatherChip(weatherHere, weatherNow)
            }
        }
    }
}

@Composable
private fun ClockChip(clock: ClockOverlay) {
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
    val (bg, fg) = overlayColors(clock.theme)
    Box(
        Modifier
            .alpha(clock.opacity.coerceIn(0f, 1f))
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

@Composable
private fun WeatherChip(weather: WeatherOverlay, now: WeatherNow) {
    val (bg, fg) = overlayColors(weather.theme)
    Row(
        Modifier
            .alpha(weather.opacity.coerceIn(0f, 1f))
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(weatherGlyph(now.weatherCode), fontSize = 44.sp)
        Spacer(Modifier.width(14.dp))
        Text(
            "${now.temperature.roundToInt()}°",
            color = fg,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
        )
        val label = weather.label
        if (!label.isNullOrBlank()) {
            Spacer(Modifier.width(14.dp))
            Text(label, color = fg.copy(alpha = 0.65f), fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** WMO weather_code → display glyph (open-meteo code table). */
private fun weatherGlyph(code: Int): String = when {
    code == 0       -> "☀"
    code in 1..3    -> "⛅"
    code in 45..48  -> "🌫"
    code in 51..67  -> "🌧"
    code in 71..77  -> "❄"
    code in 80..82  -> "🌧"
    code >= 95      -> "⛈"
    else            -> "⛅"
}

/* --- ticker overlay: full-width scrolling marquee strip -------------------- */

@Composable
private fun TickerOverlayView(ticker: TickerOverlay) {
    val (bg, fg) = overlayColors(ticker.theme)
    val align = if (ticker.position == "top") Alignment.TopCenter else Alignment.BottomCenter
    Box(Modifier.fillMaxSize(), contentAlignment = align) {
        Box(
            Modifier
                .fillMaxWidth()
                .alpha(ticker.opacity.coerceIn(0f, 1f))
                .background(bg),
        ) {
            TickerMarquee(ticker.text, ticker.speedSeconds, fg)
        }
    }
}

/**
 * Seamless marquee: the text (plus a gap) is treated as one repeating unit.
 * We lay out enough copies to cover the strip, then animate translationX from
 * 0 to -unitWidth on a linear infinite loop — the wrap frame is pixel-identical
 * to the start frame. speedSeconds = time for one full unit traverse.
 */
@Composable
private fun TickerMarquee(text: String, speedSeconds: Int, fg: Color) {
    var unitWidthPx by remember(text) { mutableIntStateOf(0) }
    val transition = rememberInfiniteTransition(label = "ticker")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = speedSeconds.coerceAtLeast(4) * 1000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tickerOffset",
    )
    BoxWithConstraints(Modifier.fillMaxWidth().clipToBounds()) {
        val stripWidthPx = constraints.maxWidth
        // Enough copies that the strip never shows a hole mid-loop.
        val copies = if (unitWidthPx > 0) (stripWidthPx / unitWidthPx) + 2 else 2
        Row(
            Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .graphicsLayer {
                    translationX = if (unitWidthPx > 0) -progress * unitWidthPx else 0f
                },
        ) {
            repeat(copies) { i ->
                Row(
                    modifier = if (i == 0) Modifier.onSizeChanged { unitWidthPx = it.width } else Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text,
                        color = fg,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                    Spacer(Modifier.width(96.dp))
                }
            }
        }
    }
}

private fun formatNow(format: String): String {
    val pattern = if (format == "24h") "HH:mm" else "h:mma"
    return LocalTime.now().format(DateTimeFormatter.ofPattern(pattern))
        .lowercase() // 3:43am style like juuno
}
