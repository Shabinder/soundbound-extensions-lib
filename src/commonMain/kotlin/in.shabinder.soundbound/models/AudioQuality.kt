package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Immutable
@Serializable
enum class AudioQuality(val kbps: String): Parcelable {
    KBPS128("128"),
    KBPS160("160"),
    KBPS192("192"),
    KBPS256("256"),
    KBPS320("320"),
    UNKNOWN("-1");

    companion object {
        fun getQuality(kbps: String): AudioQuality {
            return when (kbps) {
                "128" -> KBPS128
                "160" -> KBPS160
                "192" -> KBPS192
                "256" -> KBPS256
                "320" -> KBPS320
                "-1" -> UNKNOWN
                else -> KBPS160 // Use 160 as baseline
            }
        }
    }
}
