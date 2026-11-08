package `in`.shabinder.soundbound.providers.presence

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Optional capability: show what is playing on another service's profile ("Listening to …").
 *
 * The host coalesces calls: at most one [setPresence] per 4 s per provider, plus one right away on a track
 * change, pause or seek. Backward compatible like the other optional capabilities (old bundle ->
 * `ZiplineApiMismatchException` -> not capable).
 */
interface PresenceProvider {
  val isPresenceAvailable: Boolean get() = false

  /** Null = nothing playing. */
  suspend fun setPresence(presence: PlaybackPresence?): Unit =
    throw UnsupportedOperationException("Not supported by this source")

  suspend fun clearPresence(): Unit = throw UnsupportedOperationException("Not supported by this source")
}

@Immutable
@Serializable
data class PlaybackPresence(
  val title: String,
  val artists: List<String>,
  val album: String? = null,
  /** https only. */
  val artworkUrl: String? = null,
  val durationMs: Long,
  val positionMs: Long,
  val startedAtEpochMs: Long,
  val isPlaying: Boolean,
  val linkUrl: String? = null,
)
