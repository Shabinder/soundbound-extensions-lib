@file:OptIn(InternalSerializationApi::class)

package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass


@Serializable
@Immutable
open class ProviderConfigurationMetadata<Config : ProviderConfiguration>(
    val defaultObjectBuilder: (List<ProviderConfiguration.Data>) -> Config,
    val clazz: KClass<Config>
) {
    companion object {
        inline fun <reified T : ProviderConfiguration> buildMetadata(noinline builder: (List<ProviderConfiguration.Data>) -> T) =
            ProviderConfigurationMetadata(
                defaultObjectBuilder = builder,
                clazz = T::class
            )
    }
}

/**
 * Configuration for a [Provider]
 */
@Immutable
@Serializable
sealed interface ProviderConfiguration {

    // key to identify this configuration in soundbound
    val key: String

    val props: List<Data>

    @Immutable
    @Serializable
    data class Data(
        val key: String,
        val value: String? = null,
        val isRequired: Boolean = false,
    )

    @Immutable
    @Serializable
    object EmptyConfiguration : ProviderConfiguration {
        override val key: String = "in.shabinder.soundbound.extensions.EMPTY-CONFIG"
        override val props: List<Data> = emptyList()
    }

    @Immutable
    interface Configuration : ProviderConfiguration {
        // if userConfigurable = true, then user can change this value,
        // and soundbound will handle its configuration (except key), supported types are String, Int, Long, Boolean
        val isUserConfigurable: Boolean
    }
}

/**
 * Impl class must override [defaultConfig] if [Config] is [ProviderConfiguration.Configuration]
 *  as it will provide correct handling with [ProviderConfiguration.key] .
 *  */
interface ConfigHandler<Config : ProviderConfiguration> : Dependencies {
    /*
    * Optional Configuration which a provider might opt in to use and even make this user-configurable
    * */
    open var configuration: Config
        get() = devicePreferences.getSavedConfigOrNull(configurationMetadata)
            ?: configurationMetadata.defaultObjectBuilder(emptyList())
        set(value) {
            devicePreferences.saveConfig(value, configurationMetadata)
        }

    fun updateConfiguration(data: List<ProviderConfiguration.Data>) {
        configuration = configurationMetadata.defaultObjectBuilder(data)
    }

    val configurationMetadata: ProviderConfigurationMetadata<Config>
}