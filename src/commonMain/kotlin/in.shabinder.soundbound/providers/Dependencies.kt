package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.utils.Context
import `in`.shabinder.soundbound.utils.DevicePreferences
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope

interface Dependencies {
    val appContext: Context
    val appScope: CoroutineScope
    val devicePreferences: DevicePreferences

    /** Provides setter access in need of re-auths for default requests*/
    var httpClient: HttpClient
}
