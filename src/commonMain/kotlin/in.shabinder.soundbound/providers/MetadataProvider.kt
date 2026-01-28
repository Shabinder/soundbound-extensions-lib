package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.AppInfo
import `in`.shabinder.soundbound.models.MetadataResult
import `in`.shabinder.soundbound.models.MusicPlatform
import `in`.shabinder.soundbound.models.TrackMetadata

/**
 * Interface for metadata resolution providers.
 *
 * MetadataProviders are responsible for resolving music URLs to cross-platform
 * track metadata, including links to the same track on different streaming platforms.
 *
 * Examples: Odesli (song.link), ISRC lookup services, etc.
 *
 * Unlike [Provider] which focuses on fetching download links and playback,
 * MetadataProvider focuses on track identification and cross-platform link resolution.
 */
@Immutable
interface MetadataProvider : ZiplineService {

  /**
   * Unique identifier for this provider.
   * Example: "odesli", "songlink", "isrc-resolver"
   */
  val providerId: String

  /**
   * Human-readable name for this provider.
   * Example: "Odesli (song.link)", "ISRC Lookup"
   */
  val providerName: String

  /**
   * Priority for this provider when multiple can handle the same URL.
   * Lower values = higher priority (0-10 range recommended).
   */
  val priority: Int
    get() = 5

  /**
   * URL to the provider's logo/icon (optional).
   */
  val logoUrl: String?
    get() = null

  /**
   * List of music platforms this provider can resolve metadata for.
   * Return empty list if the provider can handle all platforms.
   */
  fun supportedPlatforms(): List<MusicPlatform>

  /**
   * Check if this provider can handle the given URL.
   */
  fun canResolve(url: String): Boolean {
    val platform = MusicPlatform.fromUrl(url)
    val supported = supportedPlatforms()
    return supported.isEmpty() || platform in supported
  }

  /**
   * Resolve a music URL to track metadata with cross-platform links.
   *
   * @param url The music URL to resolve (e.g., Spotify track link)
   * @param userCountry Optional country code for region-specific results
   * @return TrackMetadata if resolution succeeds, null if not found or error
   */
  suspend fun resolveTrackMetadata(
    url: String,
    userCountry: String? = null,
  ): TrackMetadata?

  /**
   * Resolve a music URL with detailed result including error information.
   *
   * @param url The music URL to resolve
   * @param userCountry Optional country code for region-specific results
   * @return MetadataResult containing either Success with metadata or Error with details
   */
  suspend fun resolveTrackMetadataWithResult(
    url: String,
    userCountry: String? = null,
  ): MetadataResult {
    return try {
      val metadata = resolveTrackMetadata(url, userCountry)
      if (metadata != null) {
        MetadataResult.Success(metadata)
      } else {
        MetadataResult.Error("Failed to resolve metadata for URL: $url")
      }
    } catch (e: Exception) {
      MetadataResult.Error(
        message = e.message ?: "Unknown error resolving metadata",
        code = e::class.simpleName,
      )
    }
  }

  /**
   * Initialize the provider with app information.
   * Called once when the provider is first loaded.
   */
  suspend fun init(appInfo: AppInfo) {}

  /**
   * Marker interface for providers that don't support metadata resolution.
   * Used as a sentinel to indicate the feature is not available.
   */
  interface MetadataNotAvailable : MetadataProvider {
    override val providerId: String get() = "not-available"
    override val providerName: String get() = "Not Available"
    override fun supportedPlatforms(): List<MusicPlatform> = emptyList()
    override suspend fun resolveTrackMetadata(url: String, userCountry: String?): TrackMetadata? = null
  }
}

/**
 * Configuration for a MetadataProvider as stored in the manifest.
 */
@kotlinx.serialization.Serializable
data class MetadataProviderMeta(
  /**
   * Unique source ID (e.g., "in.shabinder.soundbound.metadata.odesli")
   */
  val sourceId: String,

  /**
   * Display name (e.g., "Odesli Provider")
   */
  val sourceName: String,

  /**
   * Version code for this provider
   */
  val sourceVersionCode: Long,

  /**
   * Version name (e.g., "1.0.0")
   */
  val sourceVersionName: String,

  /**
   * URL to the zipline manifest for this provider
   */
  val link: String,

  /**
   * Whether this provider is enabled by default
   */
  val enabledByDefault: Boolean = true,

  /**
   * List of music platforms this provider supports (empty = all)
   */
  val supportedPlatforms: List<MusicPlatform> = emptyList(),
)
