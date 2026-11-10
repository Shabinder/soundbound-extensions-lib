package `in`.shabinder.soundbound.providers.stats

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.providers.scrobble.ScrobbleTrack
import kotlinx.serialization.Serializable

/**
 * Optional capability: listening statistics a service keeps for the signed-in user.
 * Backward compatible like the other optional capabilities (old bundle -> `ZiplineApiMismatchException` -> not capable).
 */
interface StatsProvider {
  val isStatsAvailable: Boolean get() = false

  suspend fun listeningStats(range: StatsRange, limit: Int = 25): ListeningStats =
    throw UnsupportedOperationException("Not supported by this source")

  /** Newest first, older than [beforeEpochSec] when set. */
  suspend fun recentListens(beforeEpochSec: Long?, limit: Int = 50): List<ScrobbleTrack> =
    throw UnsupportedOperationException("Not supported by this source")
}

@Serializable
enum class StatsRange { WEEK, MONTH, YEAR, ALL_TIME }

@Immutable
@Serializable
data class ListeningStats(
  val listenCount: Long? = null,
  val topTracks: List<StatEntry> = emptyList(),
  val topArtists: List<StatEntry> = emptyList(),
  val topAlbums: List<StatEntry> = emptyList(),
)

@Immutable
@Serializable
data class StatEntry(
  val name: String,
  val subtitle: String? = null,
  val count: Long,
  val artworkUrl: String? = null,
  val link: String? = null,
)
