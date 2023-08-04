package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.parcelize.Parcelable
import `in`.shabinder.soundbound.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Immutable
@Serializable
sealed class ProviderExceptions(
    override val message: String?,
    var extraErrorTrace: String? = null
) : Exception(message), Parcelable {

    @Parcelize
    @Immutable
    @Serializable
    data class NoMatchFound(
        val trackName: String? = null,
    ) : ProviderExceptions("$trackName : No Match Found")


    @Parcelize
    @Immutable
    @Serializable
    data class DownloadLinkFetchFailed(
        val trackName: String,
        val errorTrace: String? = null,
    ) : ProviderExceptions("$trackName : DownloadLinkFetch Failed", errorTrace)


    @Parcelize
    @Immutable
    @Serializable
    data class GeoLocationBlocked(
        val extraInfo: String? = null,
    ) : ProviderExceptions("This Content is not Accessible from your Location, try using a VPN! \nCAUSE:$extraInfo")

    @Parcelize
    @Immutable
    @Serializable
    data class LinkInvalid(
        val link: String? = null,
    ) : ProviderExceptions("Link is NOT valid.\n ${link ?: ""}")

    @Parcelize
    @Immutable
    @Serializable
    data class FeatureNotImplementedYet(
        val extraInfo: String? = null,
    ) : ProviderExceptions("Feature is not Implemented yet.", extraInfo)

    @Parcelize
    @Immutable
    @Serializable
    data class NoInternetException(
        val extraInfo: String? = null,
    ) : ProviderExceptions("Check your Internet Connectivity.", extraInfo)
}
