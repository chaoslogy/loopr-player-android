package co.loopr.player.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import co.loopr.player.LooprApp

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val app = app as LooprApp
    val identity = app.deviceStore.identity
}
