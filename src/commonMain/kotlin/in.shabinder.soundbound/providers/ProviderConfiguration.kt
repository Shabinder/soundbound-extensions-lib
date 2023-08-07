@file:OptIn(InternalSerializationApi::class)

package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.utils.GlobalJson
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import kotlin.reflect.KClass


/**
 * Configuration for a [Provider]
 */
@Immutable
@Serializable
data class ProviderConfiguration(
    val props: List<Data>,
    val isUserConfigurable: Boolean = false,
) {

    @Immutable
    @Serializable
    data class Data(
        val key: String,
        val value: String? = null,
        val isRequired: Boolean = false,
    )

    companion object {
        val EmptyConfiguration = ProviderConfiguration(props = emptyList())
    }
}


/**
 * Impl class must override [defaultConfig] if [Config] is [ProviderConfiguration.Configuration]
 *  as it will provide correct handling with [ProviderConfiguration.key] .
 *  */
interface ConfigHandler : Dependencies {

    val prefKey: String

    /*
    * Optional Configuration which a provider might opt in to use and even make this user-configurable
    * */
    open var configuration: ProviderConfiguration
        get() = with(devicePreferences) {
            getStringOrNull(prefKey)?.let {
                GlobalJson.decodeFromString(it)
            } ?: ProviderConfiguration.EmptyConfiguration
        }
        set(value) {
            devicePreferences.putString(
                prefKey,
                GlobalJson.encodeToString(value)
            )
        }

    fun updateConfiguration(data: List<ProviderConfiguration.Data>) {
        configuration = configuration.copy(props = data)
    }
}