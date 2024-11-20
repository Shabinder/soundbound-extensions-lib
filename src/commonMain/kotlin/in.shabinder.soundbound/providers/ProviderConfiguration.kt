@file:OptIn(InternalSerializationApi::class)

package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.providers.config.ConfigPropertyKey
import `in`.shabinder.soundbound.utils.GlobalJson
import kotlinx.serialization.InternalSerializationApi
import kotlin.collections.List

/**
 * Impl class must override [defaultConfig] if [Config] is [ProviderConfiguration.Configuration]
 *  as it will provide correct handling with [ProviderConfiguration.key] .
 *  */
interface ConfigHandler : Dependencies {
    val prefKey: String
//    val configs: List<ConfigPropertyKey<Any?>>
//        get() = emptyList()


    val ConfigPropertyKey<*>.prefKey: String
        get() = "$prefKey.${key}"

    fun <T> getSavedValueOrDefault(key: ConfigPropertyKey<T>, defaultValue: T): T {
        return getSavedValue(key) ?: defaultValue
    }

    fun <T> getSavedValue(key: ConfigPropertyKey<T>): T? {
        return runCatching {
            devicePreferences.getStringOrNull(key.key)?.let {
                GlobalJson.decodeFromString(key.serializer, it)
            }
        }.getOrNull()
    }

    fun <T> saveValue(key: ConfigPropertyKey<T>, value: T) {
        devicePreferences.putString(key.key, GlobalJson.encodeToString(key.serializer, value))
    }
}
