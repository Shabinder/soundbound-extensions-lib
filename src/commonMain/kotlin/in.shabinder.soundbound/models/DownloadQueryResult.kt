package `in`.shabinder.soundbound.models

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Parcelize
@Serializable
open class DownloadQueryResult(
    open val downloadURL: String,
    open val audioFormat: AudioFormat,
    open val audioQuality: AudioQuality
): Parcelable {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DownloadQueryResult) return false
        if (downloadURL != other.downloadURL) return false
        if (audioFormat != other.audioFormat) return false
        if (audioQuality != other.audioQuality) return false
        return true
    }

    override fun hashCode(): Int {
        var result = downloadURL.hashCode()
        result = 31 * result + audioFormat.hashCode()
        result = 31 * result + audioQuality.hashCode()
        return result
    }

    override fun toString(): String =
        "DownloadQueryResult(downloadURL=$downloadURL, audioFormat=$audioFormat, audioQuality=$audioQuality)"
}