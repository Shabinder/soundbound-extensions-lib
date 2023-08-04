package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.parcelize.Parcelable
import `in`.shabinder.soundbound.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Parcelize
@Serializable
@Immutable
open class DownloadQueryResult(
    open val downloadRequest: DownloadRequest,
    open val audioFormat: AudioFormat,
    open val audioQuality: AudioQuality
): Parcelable {
    @JvmOverloads
    fun copy(
        downloadRequest: Request = this.downloadRequest,
        audioFormat: AudioFormat = this.audioFormat,
        audioQuality: AudioQuality = this.audioQuality
    ): DownloadQueryResult {
        return DownloadQueryResult(
            downloadRequest = downloadRequest,
            audioFormat = audioFormat,
            audioQuality = audioQuality
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DownloadQueryResult) return false
        if (downloadRequest != other.downloadRequest) return false
        if (audioFormat != other.audioFormat) return false
        if (audioQuality != other.audioQuality) return false
        return true
    }

    override fun hashCode(): Int {
        var result = downloadRequest.hashCode()
        result = 31 * result + audioFormat.hashCode()
        result = 31 * result + audioQuality.hashCode()
        return result
    }

    override fun toString(): String =
        "DownloadQueryResult(downloadRequest=$downloadRequest, audioFormat=$audioFormat, audioQuality=$audioQuality)"
}