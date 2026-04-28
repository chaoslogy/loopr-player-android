package co.loopr.player.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.loopr.player.MainActivity

/**
 * Re-launches the player when the device finishes booting. On Fire TV the user
 * additionally pins us as the "default app" so we relaunch when Home is pressed.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                val launch = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
            }
        }
    }
}
