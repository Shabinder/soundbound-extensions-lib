package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.AppInfo
import `in`.shabinder.soundbound.models.PlatformQueryResult
import `in`.shabinder.soundbound.models.SkipSegment
import `in`.shabinder.soundbound.models.SongModel
import `in`.shabinder.soundbound.models.SourceModel
import `in`.shabinder.soundbound.providers.auth.AuthHandler
import `in`.shabinder.soundbound.providers.catalog.Catalogue
import `in`.shabinder.soundbound.providers.lyrics.LyricsProvider

@Immutable
interface Provider : ConfigHandler, Dependencies, ZiplineService, Catalogue, LyricsProvider,
    AuthHandler {

    /*
    * Preference priority
    * 0 -> 10 ,
    *   0 will make this the best extension when multiple are registered for same `source`
    * */
    val priority: Int

    /*
    * Source Name
    * ex: GAANA, JIOSAAVN, SOUNDCLOUD, etc
    * */
    val source: SourceModel

    override val prefKey: String get() = source.sourceName
    override val authDataConfigKey: String get() = source.sourceName
    val sourceLogoURL: String? get() = null

    /*
    * For a Particular URL, will return if this Extension Supports it for metadata fetching
    * */
    fun isLinkSupported(URL: String): Boolean

    suspend fun fetchPlatformQueryResult(URL: String): PlatformQueryResult

    suspend fun init(appInfo: AppInfo) {}

    val isCatalogueAvailable: Boolean
        get() = this !is Catalogue.CatalogueNotAvailable

    val isLyricsAvailable: Boolean
        get() = this !is LyricsProvider.LyricsNotAvailable

    /** True if [getSkipSegments] is implemented; hosts check this before calling it. */
    val isSkipSegmentsAvailable: Boolean get() = false

    /**
     * Optional capability: spans of [songModel] the player may skip automatically.
     *
     * Return an empty list when the track is unknown or unsupported. Keep it fast (short network
     * timeouts): the app calls it off the playback path, but a slow answer means a late skip.
     *
     * Backward compatible: extensions built before this existed have neither member in their
     * Zipline bundle, so the host's call throws `ZiplineApiMismatchException`. Callers must treat
     * any failure (of either member) as "no segments", and must not count it as provider ill health.
     */
    suspend fun getSkipSegments(songModel: SongModel): List<SkipSegment> = emptyList()
}
