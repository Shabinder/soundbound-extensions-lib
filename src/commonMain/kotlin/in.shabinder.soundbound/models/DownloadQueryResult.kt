package `in`.shabinder.soundbound.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadQueryResult(
    val downloadURL: String,
    val audioFormat: AudioFormat,
    val audioQuality: AudioQuality
)