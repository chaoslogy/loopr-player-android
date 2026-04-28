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

    /** POST /api/v1/devices/claim-code → 201 ClaimCodeResponse */
    suspend fun requestClaimCode(req: ClaimCodeRequest): Result<ClaimCodeResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = json.encodeToString(ClaimCodeRequest.serializer(), req)
                    .toRequestBody(jsonMediaType)
                val resp = client.newCall(
                    Request.Builder()
                        .url("$baseUrl/api/v1/devices/claim-code")
                        .post(body)
                        .build()
                ).execute()
                resp.use {
                    if (!it.isSuccessful) error("HTTP ${it.code} from claim-code")
                    val text = it.body?.string() ?: error("empty body")
                    json.decodeFromString(ClaimCodeResponse.serializer(), text)
                }
            }
        }

    /**
     * GET /api/v1/devices/poll/{code}
     *  - 202 → still waiting (returns null)
     *  - 200 → claimed, returns PollResponse
     *  - 404 → code unknown/expired → throws
     */
    suspend fun pollClaim(code: String): Result<PollResponse?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = client.newCall(
                    Request.Builder()
                        .url("$baseUrl/api/v1/devices/poll/${code.replace("-", "")}")
                        .get()
                        .build()
                ).execute()
                resp.use {
                    when (it.code) {
                        202 -> null
                        200 -> {
                            val text = it.body?.string() ?: error("empty body on 200 poll")
                            json.decodeFromString(PollResponse.serializer(), text)
                        }
                        else -> error("HTTP ${it.code} from poll")
                    }
                }
            }
        }
}
