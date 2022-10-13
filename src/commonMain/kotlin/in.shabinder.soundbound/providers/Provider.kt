package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.models.PlatformQueryResult
import `in`.shabinder.soundbound.models.SourceModel

abstract class Provider<Config : ProviderConfiguration>(
    dependencies: Dependencies
) : ConfigHandler<Config>, Dependencies by dependencies {


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

    /*
    * For a Particular URL, will return if this Extension Supports it
    * */
    abstract fun isLinkSupported(URL: String): Boolean

    abstract suspend fun fetchPlatformQueryResult(URL: String): PlatformQueryResult

}