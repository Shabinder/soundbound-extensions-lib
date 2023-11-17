package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads


@Immutable
@Serializable
data class DownloadQueryResult(
    val downloadRequest: Request,
    val audioFormat: AudioFormat,
    val audioQuality: AudioQuality,
    val lyrics: String? = null // actual lyrics with matching with download audio
)

@Immutable
@Serializable
data class DownloadQueryResults(
    val results: List<DownloadQueryResult>
)