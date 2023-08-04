package `in`.shabinder.soundbound.utils

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.AudioFormat
import `in`.shabinder.soundbound.models.AudioQuality
import `in`.shabinder.soundbound.providers.ProviderConfiguration
import `in`.shabinder.soundbound.providers.ProviderConfigurationMetadata


@Immutable
interface DevicePreferences {
    val preferredAudioQuality: AudioQuality

    val preferredAudioFormat: AudioFormat

    fun getSystemTimeMillis(): Long

    fun getTimeZoneId(): String

    /* Same Key used in ProviderConfiguration.Configuration*/
    fun <T: ProviderConfiguration> getSavedConfigOrNull(metadata: ProviderConfigurationMetadata<T>): T?
    fun <T: ProviderConfiguration> saveConfig(config: T, metadata: ProviderConfigurationMetadata<T>)

    fun getStringOrNull(key: String): String?
    fun saveString(key: String, value: String)


    fun getIntOrNull(key: String): Int?
    fun saveInt(key: String, value: Int)

    fun getBooleanOrNull(key: String): Boolean?
    fun saveBoolean(key: String, value: Boolean)

    fun getLongOrNull(key: String): Long?
    fun saveLong(key: String, value: Long)

    fun getFloatOrNull(key: String): Float?
    fun saveFloat(key: String, value: Float)

    fun remove(key: String)

    fun clear()
}