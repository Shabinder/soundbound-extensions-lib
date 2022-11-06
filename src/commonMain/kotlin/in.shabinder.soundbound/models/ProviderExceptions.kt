package `in`.shabinder.soundbound.models

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
sealed class ProviderExceptions(
    override val message: String?,
    var extraErrorTrace: String? = null
) : Exception(message), Parcelable {

    @Parcelize
    @Serializable
    data class NoMatchFound(
        val trackName: String? = null,
    ) : ProviderExceptions("$trackName : No Match Found")


    @Parcelize
    @Serializable
    data class DownloadLinkFetchFailed(
        val trackName: String,
        val errorTrace: String? = null,
    ) : ProviderExceptions("$trackName : DownloadLinkFetch Failed", errorTrace)


    @Parcelize
    @Serializable
    data class GeoLocationBlocked(
        val extraInfo: String? = null,
    ) : ProviderExceptions("This Content is not Accessible from your Location, try using a VPN! \nCAUSE:$extraInfo")

    @Parcelize
    @Serializable
    data class LinkInvalid(
        val link: String? = null,
    ) : ProviderExceptions("Link is NOT valid.\n ${link ?: ""}")

    @Parcelize
    @Serializable
    data class FeatureNotImplementedYet(
        val extraInfo: String? = null,
    ) : ProviderExceptions("Feature is not Implemented yet.", extraInfo)

    @Parcelize
    @Serializable
    data class NoInternetException(
        val extraInfo: String? = null,
    ) : ProviderExceptions("Check your Internet Connectivity.", extraInfo)
}
