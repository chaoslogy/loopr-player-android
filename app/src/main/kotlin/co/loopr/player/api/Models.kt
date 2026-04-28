package co.loopr.player.api

import kotlinx.serialization.Serializable

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
