package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.models.PlatformQueryResult
import `in`.shabinder.soundbound.utils.Context
import `in`.shabinder.soundbound.utils.FileUtils
import io.ktor.client.*
import kotlin.native.concurrent.ThreadLocal

interface Provider {

    /*
    * Util Classes Provided & Initialised by Core App
    * */
    var fileUtils: FileUtils
    var httpClient: HttpClient

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
    val source: String

    /*
    * For a Particular URL, will return if this Extension Supports it
    * */
    fun isLinkSupported(URL: String): Boolean


    suspend fun fetchPlatformQueryResult(URL: String): PlatformQueryResult

    /*
    * This Function is GUARANTEED to be called first and as soon as Core App Loads Extensions
    *   irrespective of rather the Extension is to be used or Not, therefore Use with care
    *
    *   - Can be used to Authenticate Clients so no delay is there when needed
    *   - Only Do things which can't be delayed or done lazily
    * */
    suspend fun initProvider(
        appContext: Context,
        fileUtils: FileUtils,
        httpClient: HttpClient
    ) {
        context = appContext
        this.fileUtils = fileUtils
        this.httpClient = httpClient
    }

    @ThreadLocal
    companion object {
        lateinit var context: Context
    }
}