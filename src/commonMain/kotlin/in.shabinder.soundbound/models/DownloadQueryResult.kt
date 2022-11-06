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

    override fun toString(): String =
        "DownloadQueryResult(downloadURL=$downloadURL, audioFormat=$audioFormat, audioQuality=$audioQuality)"
}