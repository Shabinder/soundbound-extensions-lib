package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.models.PlatformQueryResult
import io.ktor.client.HttpClient

abstract class Provider(dependencies: Dependencies) : Dependencies by dependencies {

    open val httpClient: HttpClient = HttpClient {}

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
    abstract val source: String

    /*
    * For a Particular URL, will return if this Extension Supports it
    * */
    abstract fun isLinkSupported(URL: String): Boolean


    abstract suspend fun fetchPlatformQueryResult(URL: String): PlatformQueryResult
}