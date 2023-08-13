package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.parcelize.Parcelable
import `in`.shabinder.soundbound.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Immutable
@Serializable
enum class AudioFormat: Parcelable {
    MP3, MP4, FLAC, OGG, WAV, UNKNOWN;

    companion object {
        fun getFormat(format: String): AudioFormat = runCatching {
            valueOf(format)
        }.getOrDefault(UNKNOWN)
    }
}