package `in`.shabinder.soundbound.providers.recognition

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Optional capability: identify a song from audio ("what's playing").
 *
 * Split of work: the **app** captures the microphone (or decodes a file) and computes the audio input this
 * extension asks for in [recognitionInput], because fingerprint DSP is CPU-heavy and shared between services
 * (the app has Kotlin ports of the Shazam signature and of Chromaprint). The **extension** owns what to call and
 * how: endpoint, headers, auth, response parsing. The app asks every capable extension in priority order
 * (lower first) and keeps the first match, so priority is the fallback order.
 *
 * Backward compatible: extensions built before this existed have none of these members in their Zipline bundle,
 * so the host's call throws `ZiplineApiMismatchException`; the app treats any failure as "not capable" and does
 * not count it as provider ill health.
 */
interface SongRecognizer {
  /** True if [recognizeSong] is implemented; hosts check this before calling it. */
  val isSongRecognitionAvailable: Boolean get() = false

  /** The audio form this extension needs. Only read when [isSongRecognitionAvailable]. */
  val recognitionInput: RecognitionInput get() = RecognitionInput.SHAZAM_SIGNATURE

  /** Identify [sample]. Null = the service answered "no match"; throw for transport / service errors. */
  suspend fun recognizeSong(sample: RecognitionSample): RecognitionMatch? = null
}

@Serializable
enum class RecognitionInput {
  /** Shazam signature `data:audio/vnd.shazam.sig;base64,…` of 16 kHz mono audio, at most 12 s. */
  SHAZAM_SIGNATURE,

  /** Chromaprint (algorithm 2, the `fpcalc` default) compressed base64 fingerprint of 11025 Hz mono audio. */
  CHROMAPRINT,
}

/** What the app computed for one recognition attempt. */
@Immutable
@Serializable
data class RecognitionSample(
  /** Shazam signature URI or Chromaprint fingerprint, per [RecognitionInput]. */
  val data: String,
  /** Length of the audio [data] was computed from, ms. */
  val sampleMs: Long,
  /**
   * Whole-track length in seconds when known (local files), else the sample's. AcoustID filters by it: the
   * fingerprinted part's length gives no results for a longer track.
   */
  val trackDurationSec: Long,
  /** True for a microphone capture, false for a decoded file. */
  val fromMicrophone: Boolean,
  /** ISO 3166-1 alpha-2 storefront hint, e.g. "IN". */
  val country: String = "US",
  /** BCP 47, e.g. "en-US". */
  val language: String = "en-US",
  /** Per-service credential the app holds (e.g. the AcoustID application key); never logged. */
  val apiKey: String? = null,
)

/** One identified song. Only [title] and [artist] are required. */
@Immutable
@Serializable
data class RecognitionMatch(
  val title: String,
  val artist: String,
  val isrc: String? = null,
  val album: String? = null,
  val year: String? = null,
  val durationSec: Long? = null,
  val coverArtUrl: String? = null,
  /** Links that identify the song on a platform, best first (Apple Music, Spotify track, service page). */
  val links: Map<String, String> = emptyMap(),
  /** Service ids worth keeping, e.g. "shazamKey", "appleMusicId", "musicBrainzId". */
  val ids: Map<String, String> = emptyMap(),
  /** Where in the song the sample matched, seconds. */
  val offsetSec: Double? = null,
  /** Service confidence 0..1 when it reports one. */
  val score: Double? = null,
) {
  companion object {
    /** The recognizer's own page for the match. Every other [links] key is the extension's own choice. */
    const val LINK_SERVICE = "service"
    /** MusicBrainz recording id: the host's portable recording identifier (like an ISRC), not a source. */
    const val ID_MUSICBRAINZ = "musicBrainzId"

    @Deprecated("Link and id keys other than LINK_SERVICE / ID_MUSICBRAINZ belong to the extension; declare your own.")
    const val LINK_APPLE_MUSIC = "appleMusic"
    @Deprecated("Link and id keys other than LINK_SERVICE / ID_MUSICBRAINZ belong to the extension; declare your own.")
    const val LINK_SPOTIFY = "spotify"
    @Deprecated("Link and id keys other than LINK_SERVICE / ID_MUSICBRAINZ belong to the extension; declare your own.")
    const val ID_SHAZAM = "shazamKey"
    @Deprecated("Link and id keys other than LINK_SERVICE / ID_MUSICBRAINZ belong to the extension; declare your own.")
    const val ID_APPLE_MUSIC = "appleMusicId"
  }
}
