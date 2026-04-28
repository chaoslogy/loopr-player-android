package co.loopr.player

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import co.loopr.player.api.LooprApi
import co.loopr.player.data.DeviceStore

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loopr-prefs")

class LooprApp : Application() {
    lateinit var api: LooprApi
        private set
    lateinit var deviceStore: DeviceStore
        private set

    override fun onCreate() {
        super.onCreate()
        api = LooprApi(BuildConfig.API_BASE_URL)
        deviceStore = DeviceStore(dataStore)
    }
}
