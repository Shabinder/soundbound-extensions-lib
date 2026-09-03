package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class QualityOption(
    val quality: AudioQuality,
    val label: String,
    val description: String? = null,
    val approxBitDepth: Int? = null,
    val approxSampleRateHz: Int? = null,
) {
    val isLossless: Boolean get() = quality.isLossless
}
