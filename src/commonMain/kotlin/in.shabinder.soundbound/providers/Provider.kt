package `in`.shabinder.soundbound.providers

import app.cash.zipline.Zipline
import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.PlatformQueryResult
import `in`.shabinder.soundbound.models.SourceModel
import `in`.shabinder.soundbound.providers.catalog.Catalogue
import kotlinx.coroutines.Dispatchers

interface Provider : ConfigHandler, Dependencies, ZiplineService, Catalogue {

    /*
    * Preference priority
    * 0 -> 10 ,
    *   0 will make this the best extension when multiple are registered for same `source`
    * */
    abstract val priority: Int

    /*
    * Source Name
    * ex: GAANA, JIOSAAVN, SOUNDCLOUD, etc
    * */
    abstract val source: SourceModel

    override val prefKey: String get() = source.sourceName

    /*
    * For a Particular URL, will return if this Extension Supports it for metadata fetching
    * */
    abstract fun isLinkSupported(URL: String): Boolean

    abstract suspend fun fetchPlatformQueryResult(URL: String): PlatformQueryResult

    open val isCatalogueAvailable: Boolean
        get() = (this as Catalogue) !is Catalogue.CatalogueNotAvailable
}