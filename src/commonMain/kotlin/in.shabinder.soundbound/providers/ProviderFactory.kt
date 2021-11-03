package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.utils.FileUtils
import io.ktor.client.*

interface ProviderFactory {
    /*
    * This function will create all Providers and Init them with parameters accepted by this function
    * */
    fun createProviders(
        fileUtils: FileUtils,
        httpClient: HttpClient
    ): List<Provider>
}