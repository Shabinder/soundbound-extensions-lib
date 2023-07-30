package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.utils.Context
import `in`.shabinder.soundbound.utils.DevicePreferences
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import kotlinx.coroutines.CoroutineScope

@Immutable
interface Dependencies : ZiplineService {
    val appContext: Context
    val appScope: CoroutineScope
    val devicePreferences: DevicePreferences
    // val desECBDecrypt: (key: String, input: String) -> String
    val httpClientBuilder: (
        usePreConfig: Boolean,
        block: HttpClientConfig<HttpClientEngineConfig>.() -> Unit
    ) -> HttpClient
}
