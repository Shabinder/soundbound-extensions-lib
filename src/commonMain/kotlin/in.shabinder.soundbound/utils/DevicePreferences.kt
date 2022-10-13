package `in`.shabinder.soundbound.utils

import `in`.shabinder.soundbound.models.AudioFormat
import `in`.shabinder.soundbound.models.AudioQuality
import `in`.shabinder.soundbound.providers.ProviderConfiguration
import `in`.shabinder.soundbound.providers.ProviderConfigurationMetadata

interface DevicePreferences {
    val preferredAudioQuality: AudioQuality

    val preferredAudioFormat: AudioFormat

    /* Same Key used in ProviderConfiguration.Configuration*/
    fun <T: ProviderConfiguration> getSavedConfigOrNull(metadata: ProviderConfigurationMetadata<T>): T?
    fun <T: ProviderConfiguration> saveConfig(config: T, metadata: ProviderConfigurationMetadata<T>)
}