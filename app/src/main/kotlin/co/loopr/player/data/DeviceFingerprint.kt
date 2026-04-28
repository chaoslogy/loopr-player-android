package co.loopr.player.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

/**
 * A stable, anonymous identifier for this physical device. Used in the claim-code
 * request so the backend can associate consecutive code requests with the same TV.
 * Combines ANDROID_ID with hardware identifiers for stability across reboots,
 * sha256-truncated for opacity. Not a security primitive — just a stable id.
 */
object DeviceFingerprint {
    @Suppress("HardwareIds")
    fun get(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: ""
        val seed = "$androidId|${Build.MANUFACTURER}|${Build.MODEL}|${Build.HARDWARE}|${Build.SERIAL.takeIf { it != "unknown" } ?: ""}"
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
        return digest.take(16).joinToString("") { "%02x".format(it) }   // 32 hex chars
    }

    fun hardwareModel(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}
