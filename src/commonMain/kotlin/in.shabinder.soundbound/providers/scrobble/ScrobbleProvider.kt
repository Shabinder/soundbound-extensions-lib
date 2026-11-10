package `in`.shabinder.soundbound.providers.scrobble

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Optional capability: listen history (now playing, scrobbles, loves) to a service.
 *
 * The app owns the generic parts: the "counts as a listen" threshold, the offline queue and backoff. It fans
 * out to every enabled, signed-in provider with [isScrobbleAvailable].
 *
 * Backward compatible: bundles built before this have none of these members, so the host's call throws
 * `ZiplineApiMismatchException`; treat that as "not capable", never as provider ill health.
 */
interface ScrobbleProvider {
  val isScrobbleAvailable: Boolean get() = false

  /** Most tracks one [scrobble] call takes. */
  val scrobbleBatchMax: Int get() = 50

  suspend fun nowPlaying(track: ScrobbleTrack): Unit = unsupported()

  /** `playedAtEpochSec` is set on every track. Must be idempotent per (track, playedAt): the host retries. */
  suspend fun scrobble(tracks: List<ScrobbleTrack>): ScrobbleResult = unsupported()

  suspend fun love(track: ScrobbleTrack, loved: Boolean): Unit = unsupported()
}

@Immutable
@Serializable
data class ScrobbleTrack(
  val title: String,
  val artists: List<String>,
  val album: String? = null,
  val durationMs: Long,
  val playedAtEpochSec: Long? = null,
  val isrc: String? = null,
  val mbid: String? = null,
  /** The song's own link in the app (any source). */
  val originUrl: String,
)

@Immutable
@Serializable
data class ScrobbleResult(val accepted: Int, val status: Status, val message: String = "") {
  @Serializable
  enum class Status { OK, RETRY_LATER, AUTH_FAILED, REJECTED }
}

internal fun unsupported(): Nothing = throw UnsupportedOperationException("Not supported by this source")
