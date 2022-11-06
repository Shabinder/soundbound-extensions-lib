package `in`.shabinder.soundbound.models

import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
sealed class DownloadStatus {

    @Parcelize
    @Serializable
    object Downloaded : DownloadStatus()

    @Parcelize
    @Serializable
    data class Downloading(val progress: Int = 2) : DownloadStatus()

    @Parcelize
    @Serializable
    object Queued : DownloadStatus()

    @Parcelize
    @Serializable
    object NotDownloaded : DownloadStatus()

    @Parcelize
    @Serializable
    object Converting : DownloadStatus()

    @Parcelize
    @Serializable
    data class Failed(@Contextual val error: Throwable) : DownloadStatus()
}
