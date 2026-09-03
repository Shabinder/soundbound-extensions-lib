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
    open val lyrics: String? = null, // actual lyrics with matching with download audio
    open val bitDepth: Int? = null,
    open val sampleRateHz: Int? = null,
    open val codec: String? = null,
    open val audioQualityLabel: String? = null, // provider-declared human label, e.g. "24-bit/96kHz"
    open val playbackDelivery: PlaybackDelivery = PlaybackDelivery.DIRECT_HTTP,
) {
    @JvmOverloads
    open fun copy(
        downloadRequest: Request = this.downloadRequest,
        audioFormat: AudioFormat = this.audioFormat,
        audioQuality: AudioQuality = this.audioQuality,
        lyrics: String? = this.lyrics,
        bitDepth: Int? = this.bitDepth,
        sampleRateHz: Int? = this.sampleRateHz,
        codec: String? = this.codec,
        audioQualityLabel: String? = this.audioQualityLabel,
        playbackDelivery: PlaybackDelivery = this.playbackDelivery,
    ): DownloadQueryResult {
        return DownloadQueryResult(
            downloadRequest = downloadRequest,
            audioFormat = audioFormat,
            audioQuality = audioQuality,
            lyrics = lyrics,
            bitDepth = bitDepth,
            sampleRateHz = sampleRateHz,
            codec = codec,
            audioQualityLabel = audioQualityLabel,
            playbackDelivery = playbackDelivery,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DownloadQueryResult) return false
        if (downloadRequest != other.downloadRequest) return false
        if (audioFormat != other.audioFormat) return false
        if (audioQuality != other.audioQuality) return false
        if (lyrics != other.lyrics) return false
        if (bitDepth != other.bitDepth) return false
        if (sampleRateHz != other.sampleRateHz) return false
        if (codec != other.codec) return false
        if (audioQualityLabel != other.audioQualityLabel) return false
        if (playbackDelivery != other.playbackDelivery) return false
        return true
    }

    override fun hashCode(): Int {
        var result = downloadRequest.hashCode()
        result = 31 * result + audioFormat.hashCode()
        result = 31 * result + audioQuality.hashCode()
        result = 31 * result + lyrics.hashCode()
        result = 31 * result + (bitDepth ?: 0)
        result = 31 * result + (sampleRateHz ?: 0)
        result = 31 * result + (codec?.hashCode() ?: 0)
        result = 31 * result + (audioQualityLabel?.hashCode() ?: 0)
        result = 31 * result + playbackDelivery.hashCode()
        return result
    }

    override fun toString(): String =
        "DownloadQueryResult(downloadRequest=$downloadRequest, audioFormat=$audioFormat, audioQuality=$audioQuality, lyrics=$lyrics, bitDepth=$bitDepth, sampleRateHz=$sampleRateHz, codec=$codec, audioQualityLabel=$audioQualityLabel, playbackDelivery=$playbackDelivery)"
}

@Serializable
enum class PlaybackDelivery {
    DIRECT_HTTP,
    EXTENSION_SOURCE,
}

@Immutable
@Serializable
data class DownloadQueryResults(
    val results: List<DownloadQueryResult>
)

@Immutable
@Serializable
data class SongModels(
    val results: List<SongModel>
)
