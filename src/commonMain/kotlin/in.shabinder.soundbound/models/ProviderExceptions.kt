package `in`.shabinder.soundbound.models

import kotlinx.serialization.Serializable

@Serializable
sealed class ProviderExceptions(override val message: String?) : Exception(message) {

    var extraErrorTrace: String? = null

    constructor(message: String?, extraErrorTrace: String?) : this(message) {
        this.extraErrorTrace = extraErrorTrace
    }

    @Serializable
    data class NoMatchFound(
        val trackName: String? = null,
    ) : ProviderExceptions("$trackName : No Match Found")


    @Serializable
    data class DownloadLinkFetchFailed(
        val trackName: String,
        val errorTrace: String? = null,
    ) : ProviderExceptions("$trackName : DownloadLinkFetch Failed", errorTrace)


    @Serializable
    data class GeoLocationBlocked(
        val extraInfo: String? = null,
    ) : ProviderExceptions("This Content is not Accessible from your Location, try using a VPN! \nCAUSE:$extraInfo")

    @Serializable
    data class LinkInvalid(
        val link: String? = null,
    ) : ProviderExceptions("Link is NOT valid.\n ${link ?: ""}")

    @Serializable
    data class FeatureNotImplementedYet(
        val extraInfo: String? = null,
    ) : ProviderExceptions("Feature is not Implemented yet.", extraInfo)

    @Serializable
    data class NoInternetException(
        val extraInfo: String? = null,
    ) : ProviderExceptions("Check your Internet Connectivity.", extraInfo)
}
