package co.loopr.player.api

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
        )

        @Serializable
        data class Widget(
            val id: Long,
            val kind: String,                 // "web_url", "youtube", etc.
            val name: String,
            val configJson: String,
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
