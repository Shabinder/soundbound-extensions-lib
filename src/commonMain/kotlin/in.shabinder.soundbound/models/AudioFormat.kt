package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable


@Immutable
@Serializable
enum class AudioFormat: Comparable<AudioFormat> {
    MP3, MP4, FLAC, OGG, WAV, WEBM, WEBA, ALAC, UNKNOWN;

    val isLossless: Boolean
        get() = this == FLAC || this == WAV || this == ALAC

    companion object {
        fun getFormat(format: String): AudioFormat = runCatching {
            valueOf(format)
        }.getOrDefault(UNKNOWN)
    }
}