package `in`.shabinder.soundbound.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class DownloadStatus {

    @Serializable
    object Downloaded : DownloadStatus()

    @Serializable
    data class Downloading(val progress: Int = 2) : DownloadStatus()

    @Serializable
    object Queued : DownloadStatus()

    @Serializable
    object NotDownloaded : DownloadStatus()

    @Serializable
    object Converting : DownloadStatus()

    @Serializable
    data class Failed(@Contextual val error: Throwable) : DownloadStatus()
}
