package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Parcelize
@Immutable
@Serializable
sealed class DownloadStatus: Parcelable {

    @Parcelize
    @Immutable
    @Serializable
    object Downloaded : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    data class Downloading(val progress: Int = 2) : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    object Queued : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    object NotDownloaded : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    object Converting : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    data class Failed(@Contextual val error: Throwable) : DownloadStatus()
}
