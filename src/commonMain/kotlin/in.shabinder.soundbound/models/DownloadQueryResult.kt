package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads


@Serializable
@Immutable
open class DownloadQueryResult(
    open val downloadRequest: DownloadRequest,
    open val audioFormat: AudioFormat,
    open val audioQuality: AudioQuality,
    open val lyrics: String? = null // actual lyrics with matching with download audio
) {
    @JvmOverloads
    open fun copy(
        downloadRequest: Request = this.downloadRequest,
        audioFormat: AudioFormat = this.audioFormat,
        audioQuality: AudioQuality = this.audioQuality,
        lyrics: String? = this.lyrics,
    ): DownloadQueryResult {
        return DownloadQueryResult(
            downloadRequest = downloadRequest,
            audioFormat = audioFormat,
            audioQuality = audioQuality,
            lyrics = lyrics
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DownloadQueryResult) return false
        if (downloadRequest != other.downloadRequest) return false
        if (audioFormat != other.audioFormat) return false
        if (audioQuality != other.audioQuality) return false
        if (lyrics != other.lyrics) return false
        return true
    }

    override fun hashCode(): Int {
        var result = downloadRequest.hashCode()
        result = 31 * result + audioFormat.hashCode()
        result = 31 * result + audioQuality.hashCode()
        result = 31 * result + lyrics.hashCode()
        return result
    }

    override fun toString(): String =
        "DownloadQueryResult(downloadRequest=$downloadRequest, audioFormat=$audioFormat, audioQuality=$audioQuality, lyrics=$lyrics)"
}