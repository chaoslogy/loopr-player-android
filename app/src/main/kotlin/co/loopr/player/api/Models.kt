package co.loopr.player.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ClaimCodeRequest(
    val deviceFingerprint: String,
    val hardwareModel: String? = null,
    val appVersion: String? = null,
)

@Serializable
data class ClaimCodeResponse(
    val code: String,
    val pairUrl: String,
    val expiresAt: String,
    val pollIntervalSeconds: Long,
)

@Serializable
data class PollResponse(
    val screenId: Long,
    val workspaceId: Long,
    val screenName: String,
    val deviceToken: String,
    val pairedAt: String,
)

@Serializable
data class AssignedPlaylistView(
    val screen: ScreenIdentity,
    val playlist: Playlist? = null,
) {
    @Serializable
    data class ScreenIdentity(
        val id: Long,
        val workspaceId: Long,
        val name: String,
        val currentPlaylistId: String? = null,
        // "landscape" (default) | "portrait" | "portrait_flipped". Optional —
        // older servers don't send it, in which case we stay landscape.
        val orientation: String? = null,
        // Free-form JSON: { "clock": {...}, "weather": {...} }. Player parses what it knows.
        val overlays: JsonElement? = null,
    )

    @Serializable
    data class Playlist(
        val id: Long,
        val name: String,
        val items: List<Item>,
    ) {
        @Serializable
        data class Item(
            val id: Long,
            val position: Int,
            val kind: String,                 // "media" | "widget"
            val durationSeconds: Int,
            val transition: String,
            val widget: Widget? = null,
            val media:  Media?  = null,       // populated when kind="media"
        )

        @Serializable
        data class Widget(
            val id: Long,
            val kind: String,                 // "web_url", "youtube", etc.
            val name: String,
            val configJson: String,
        )

        @Serializable
        data class Media(
            val id: Long,
            val kind: String,                 // "image" | "video" | "gif"
            val name: String,
            val contentType: String? = null,
            val byteSize: Long? = null,
            val width: Int? = null,
            val height: Int? = null,
            val durationMs: Int? = null,
            val publicUrl: String,            // directly fetchable, no auth
        )
    }
}

/**
 * Typed view of the "clock" overlay subtree. Built lazily off [AssignedPlaylistView.ScreenIdentity.overlays]
 * so unknown overlay kinds don't break deserialization.
 */
@Serializable
data class ClockOverlay(
    val enabled: Boolean = false,
    val format: String = "12h",        // "12h" | "24h"
    val theme: String = "light",       // "light" | "dark"
    val opacity: Float = 0.95f,        // 0..1
    val position: String = "bottom-right" // "top-left" | "top-right" | "bottom-left" | "bottom-right"
)

/**
 * Typed view of the "weather" overlay subtree. Same lenient-parse contract as [ClockOverlay].
 * Current conditions come from open-meteo (keyless); the chip hides until the first fetch lands.
 */
@Serializable
data class WeatherOverlay(
    val enabled: Boolean = false,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val label: String? = null,         // e.g. "London", shown next to the temperature
    val units: String = "c",           // "c" | "f"
    val theme: String = "light",       // "light" | "dark"
    val opacity: Float = 0.95f,        // 0..1
    val position: String = "bottom-right" // "top-left" | "top-right" | "bottom-left" | "bottom-right"
)

/**
 * Typed view of the "ticker" overlay subtree — a full-width scrolling text strip.
 */
@Serializable
data class TickerOverlay(
    val enabled: Boolean = false,
    val text: String = "",
    val position: String = "bottom",   // "top" | "bottom"
    val theme: String = "light",       // "light" | "dark"
    val speedSeconds: Int = 20,        // seconds for one full traverse of the text
    val opacity: Float = 0.95f,        // 0..1
)

/** Subset of the open-meteo /v1/forecast response the player cares about. */
@Serializable
data class OpenMeteoCurrentResponse(
    val current: Current? = null,
) {
    @Serializable
    data class Current(
        @SerialName("temperature_2m") val temperature2m: Double? = null,
        @SerialName("weather_code") val weatherCode: Int? = null,
    )
}

@Serializable
data class UrlSessionCredentials(
    val sessionId: Long,
    val targetUrl: String,
    val cookieDomain: String? = null,
    val cookies: List<Cookie> = emptyList(),
    val localStorage: kotlinx.serialization.json.JsonElement? = null,
    val sessionStorage: kotlinx.serialization.json.JsonElement? = null,
) {
    @Serializable
    data class Cookie(
        val name: String,
        val value: String,
        val domain: String? = null,
        val path: String = "/",
        val expires: Long? = null,
        val httpOnly: Boolean = false,
        val secure: Boolean = true,
        val sameSite: String = "Lax",
    )
}
