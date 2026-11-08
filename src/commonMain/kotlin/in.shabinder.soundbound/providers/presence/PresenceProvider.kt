package `in`.shabinder.soundbound.providers.presence

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Optional capability: publish what the user is listening to somewhere else (a chat app's status, a profile).
 *
 * The **host** decides when to call: it watches the player and fans out to every enabled provider, coalescing so a
 * provider sees at most one call per 4 s (well inside Discord's documented 5 updates / 20 s), sent as soon as that
 * spacing allows after a track change, pause or seek. The **extension** owns the transport (HTTP, [webSocket], a
 * local IPC socket) and the payload.
 *
 * Backward compatible: bundles built before this existed lack these members, so the host's call throws
 * `ZiplineApiMismatchException`; hosts treat any failure as "not capable".
 */
interface PresenceProvider {
  /** True if [setPresence] is implemented; hosts check this before calling it. */
  val isPresenceAvailable: Boolean get() = false

  /** Null = nothing playing. Paused playback arrives with [PlaybackPresence.isPlaying] false. */
  suspend fun setPresence(presence: PlaybackPresence?): Unit = unsupported()

  /** The user turned presence off for this provider, or it is being unloaded: remove anything published. */
  suspend fun clear(): Unit = unsupported()
}

@Immutable
@Serializable
data class PlaybackPresence(
  val title: String,
  val artists: List<String>,
  val album: String? = null,
  /** https only; the host drops other schemes. */
  val artworkUrl: String? = null,
  val durationMs: Long,
  /** Position when this snapshot was taken. */
  val positionMs: Long,
  /** Wall clock (epoch ms) at which the track would have started had it played without pauses: now - position. */
  val startedAtEpochMs: Long,
  val isPlaying: Boolean,
  /** The track's link, when it has a public one. */
  val linkUrl: String? = null,
)

private fun unsupported(): Nothing = throw UnsupportedOperationException("Presence is not supported by this source")
