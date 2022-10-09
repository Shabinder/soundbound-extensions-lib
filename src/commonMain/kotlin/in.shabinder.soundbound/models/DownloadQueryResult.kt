package `in`.shabinder.soundbound.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
open class DownloadQueryResult(
    open val downloadURL: String,
    open val audioFormat: AudioFormat,
    open val audioQuality: AudioQuality
) {
    @JvmOverloads
    fun copy(
        downloadURL: String = this.downloadURL,
        audioFormat: AudioFormat = this.audioFormat,
        audioQuality: AudioQuality = this.audioQuality
    ): DownloadQueryResult {
        return DownloadQueryResult(
            downloadURL = downloadURL,
            audioFormat = audioFormat,
            audioQuality = audioQuality
        )
    }
}