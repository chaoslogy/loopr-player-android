package co.loopr.player.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class LooprApi(private val baseUrl: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun requestClaimCode(req: ClaimCodeRequest): Result<ClaimCodeResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = json.encodeToString(ClaimCodeRequest.serializer(), req)
                    .toRequestBody(jsonMediaType)
                client.newCall(
                    Request.Builder().url("$baseUrl/api/v1/devices/claim-code").post(body).build()
                ).execute().use {
                    if (!it.isSuccessful) error("HTTP ${it.code} from claim-code")
                    json.decodeFromString(ClaimCodeResponse.serializer(), it.body?.string() ?: error("empty body"))
                }
            }
        }

    suspend fun pollClaim(code: String): Result<PollResponse?> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(
                    Request.Builder().url("$baseUrl/api/v1/devices/poll/${code.replace("-", "")}").get().build()
                ).execute().use {
                    when (it.code) {
                        202 -> null
                        200 -> json.decodeFromString(PollResponse.serializer(),
                                                     it.body?.string() ?: error("empty body on 200 poll"))
                        else -> error("HTTP ${it.code} from poll")
                    }
                }
            }
        }

    /** GET /api/v1/me/playlist — Authorization: Bearer <device-token>. */
    suspend fun fetchAssignedPlaylist(deviceToken: String): Result<AssignedPlaylistView> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(
                    Request.Builder()
                        .url("$baseUrl/api/v1/me/playlist")
                        .addHeader("Authorization", "Bearer $deviceToken")
                        .get()
                        .build()
                ).execute().use {
                    if (!it.isSuccessful) error("HTTP ${it.code} from /me/playlist")
                    json.decodeFromString(AssignedPlaylistView.serializer(),
                                           it.body?.string() ?: error("empty body on /me/playlist"))
                }
            }
        }

    /**
     * GET /api/v1/me/url-sessions/{id}/credentials — decrypted cookies + storage,
     * device-token-authenticated. Returns null if the session is gone or expired
     * so the player can show 'Login required'.
     */
    suspend fun fetchUrlSessionCredentials(deviceToken: String, sessionId: Long): Result<UrlSessionCredentials?> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(
                    Request.Builder()
                        .url("$baseUrl/api/v1/me/url-sessions/$sessionId/credentials")
                        .addHeader("Authorization", "Bearer $deviceToken")
                        .get()
                        .build()
                ).execute().use {
                    when (it.code) {
                        200 -> json.decodeFromString(
                            UrlSessionCredentials.serializer(),
                            it.body?.string() ?: error("empty body on credentials")
                        )
                        404, 410, 403 -> null
                        else -> error("HTTP ${'$'}{it.code} from /me/url-sessions/$sessionId/credentials")
                    }
                }
            }
        }
}
