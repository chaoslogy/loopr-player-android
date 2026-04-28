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

/**
 * Returned by GET /api/v1/devices/poll/{code} when the dashboard has claimed the code.
 * The 202 (waiting) and 404 (expired/unknown) cases are conveyed through HTTP status,
 * and the client deserialises this DTO only on 200.
 */
@Serializable
data class PollResponse(
    val screenId: Long,
    val workspaceId: Long,
    val screenName: String,
    val deviceToken: String,    // raw token; persisted to DataStore
    val pairedAt: String,
)
