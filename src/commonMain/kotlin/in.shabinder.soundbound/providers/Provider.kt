package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.AppInfo
import `in`.shabinder.soundbound.models.PlatformQueryResult
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
}
