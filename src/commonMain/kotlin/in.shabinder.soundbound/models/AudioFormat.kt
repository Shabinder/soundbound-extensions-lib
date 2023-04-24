package `in`.shabinder.soundbound.models

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
enum class AudioFormat: Parcelable {
    MP3, MP4, FLAC, OGG, WAV, UNKNOWN;

    companion object {
        fun getFormat(format: String): AudioFormat = runCatching {
            valueOf(format)
        }.getOrDefault(UNKNOWN)
    }
}