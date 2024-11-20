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

    suspend fun <T : ConfigPropertyKey<*>> getConfigKeys(): List<T> = emptyList()

    val ConfigPropertyKey<*>.prefKey: String
        get() = "$prefKey.${key}"
}

fun <T> ConfigHandler.getSavedValueOrDefault(key: ConfigPropertyKey<T>, defaultValue: T): T {
    return getSavedValue(key) ?: defaultValue
}

fun <T> ConfigHandler.getSavedValue(key: ConfigPropertyKey<T>): T? {
    return runCatching {
        devicePreferences.getStringOrNull(key.prefKey)?.let {
            GlobalJson.decodeFromString(key.serializer, it)
        }
    }.getOrNull()
}

fun <T> ConfigHandler.saveValue(key: ConfigPropertyKey<T>, value: T) {
    devicePreferences.putString(key.prefKey, GlobalJson.encodeToString(key.serializer, value))
}
