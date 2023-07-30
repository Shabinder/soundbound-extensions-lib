@file:OptIn(InternalSerializationApi::class)

package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Contextual
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.serializer
import kotlin.reflect.KClass


@Serializable
@Immutable
data class ProviderConfigurationMetadata<Config : ProviderConfiguration>(
    val defaultObject: Config,
    val clazz: KClass<Config>
) {
    companion object {
        inline fun <reified T : ProviderConfiguration> buildMetadata(builder: () -> T) =
            ProviderConfigurationMetadata(
                defaultObject = builder(),
                clazz = T::class
            )
    }
}

/**
 * Configuration for a [Provider]
 */
@Immutable
@Serializable
sealed class ProviderConfiguration {

    // key to identify this configuration in soundbound
    abstract val key: String

    @Immutable
    @Serializable
    object EmptyConfiguration : ProviderConfiguration() {
        override val key: String = "in.shabinder.soundbound.extensions.EMPTY-CONFIG"
    }

    @Immutable
    @Serializable
    abstract class Configuration : ProviderConfiguration() {
        // if userConfigurable = true, then user can change this value,
        // and soundbound will handle its configuration (except key), supported types are String, Int, Long, Boolean
        open val isUserConfigurable: Boolean = false
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
    open var configuration: Config?
        get() = devicePreferences.getSavedConfigOrNull(
            configurationMetadata
        ) ?: runCatching {
            configurationMetadata.defaultObject
        }.getOrNull()
        set(value) {
            if (value != null) devicePreferences.saveConfig(value, configurationMetadata)
        }

    val configurationMetadata: ProviderConfigurationMetadata<Config>
}