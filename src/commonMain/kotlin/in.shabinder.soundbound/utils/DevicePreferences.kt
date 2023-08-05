package `in`.shabinder.soundbound.utils

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.AudioFormat
import `in`.shabinder.soundbound.models.AudioQuality
import `in`.shabinder.soundbound.providers.ProviderConfiguration
import `in`.shabinder.soundbound.providers.ProviderConfigurationMetadata


@Immutable
abstract class DevicePreferences {
    abstract val preferredAudioQuality: AudioQuality

    abstract val preferredAudioFormat: AudioFormat

    fun getSystemTimeMillis(): Long

    fun getTimeZoneId(): String

    /* Same Key used in ProviderConfiguration.Configuration*/
    fun <T: ProviderConfiguration> getSavedConfigOrNull(metadata: ProviderConfigurationMetadata<T>): T?
    fun <T: ProviderConfiguration> saveConfig(config: T, metadata: ProviderConfigurationMetadata<T>)

    fun getStringOrNull(key: String): String?
    fun putString(key: String, value: String)


    fun getIntOrNull(key: String): Int?
    fun putInt(key: String, value: Int)

    fun getBooleanOrNull(key: String): Boolean?
    fun putBoolean(key: String, value: Boolean)

    fun getLongOrNull(key: String): Long?
    fun putLong(key: String, value: Long)

    fun getFloatOrNull(key: String): Float?
    fun putFloat(key: String, value: Float)

    fun remove(key: String)

    fun clear()
}