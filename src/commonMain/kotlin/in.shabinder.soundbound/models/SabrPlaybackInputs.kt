package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Volatile YouTube player inputs needed to start an app-side SABR session.
 *
 * The extension owns acquiring and deciphering these values. The app owns the stable UMP/SABR
 * transport. Keeping this payload on [DownloadQueryResult] makes the SABR path additive: the
 * classic [DownloadQueryResult.downloadRequest] remains the default and fallback.
 */
@Immutable
@Serializable
data class SabrPlaybackInputs(
  val videoId: String,
  val serverAbrStreamingUrl: String,
  val videoPlaybackUstreamerConfig: String,
  val poToken: String,
  val formats: List<SabrFormat>,
  val selectedAudioItag: Int,
  val clientName: Int,
  val clientVersion: String,
  val userAgent: String,
  val durationMs: Long,
  val contentLength: Long,
)

/** Format identity fields sent back to YouTube in a VideoPlaybackAbrRequest. */
@Immutable
@Serializable
data class SabrFormat(
  val itag: Int,
  val lastModified: Long? = null,
  val xtags: String? = null,
  val mimeType: String,
  val bitrate: Int,
  val contentLength: Long? = null,
  val approxDurationMs: Long? = null,
  val audioTrackId: String? = null,
  val isDrc: Boolean = false,
)
