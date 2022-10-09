package `in`.shabinder.soundbound.utils

import `in`.shabinder.soundbound.models.AudioFormat
import `in`.shabinder.soundbound.models.AudioQuality

interface DevicePreferences {
    val preferredAudioQuality: AudioQuality

    val preferredAudioFormat: AudioFormat
}