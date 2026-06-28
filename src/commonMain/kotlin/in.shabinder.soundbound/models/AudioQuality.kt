package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable


@Immutable
@Serializable
enum class AudioQuality(val kbps: String) {
    KBPS128("128"),
    KBPS160("160"),
    KBPS192("192"),
    KBPS256("256"),
    KBPS320("320"),

    // Lossless tiers. The kbps values are the representative stereo bitrates
    // (16/44.1 = 1411, 24/96 = 4608, 24/192 = 9216) so the existing
    // kbps.toInt() comparator sorts them above the lossy tiers unchanged.
    LOSSLESS("1411"),
    HI_RES("4608"),
    HI_RES_MAX("9216"),

    UNKNOWN("-1");

    val isLossless: Boolean
        get() = this == LOSSLESS || this == HI_RES || this == HI_RES_MAX

    companion object {
        val qualityComparator = Comparator<AudioQuality> { r1, r2 ->
            r1.kbps.toInt() - r2.kbps.toInt()
        }

        fun getQuality(kbps: String): AudioQuality {
            return when (kbps) {
                "128" -> KBPS128
                "160" -> KBPS160
                "192" -> KBPS192
                "256" -> KBPS256
                "320" -> KBPS320
                "1411" -> LOSSLESS
                "4608" -> HI_RES
                "9216" -> HI_RES_MAX
                "-1" -> UNKNOWN
                else -> KBPS160 // Use 160 as baseline
            }
        }
    }
}
