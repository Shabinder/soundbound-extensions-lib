package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable


@Immutable
@Serializable
enum class AudioFormat {
    MP3, MP4, FLAC, OGG, WAV, UNKNOWN;

    companion object {
        fun getFormat(format: String): AudioFormat = runCatching {
            valueOf(format)
        }.getOrDefault(UNKNOWN)
    }
}