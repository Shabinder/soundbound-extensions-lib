package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.diagnostics.DiagnosticsData


import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed class ProviderExceptions(
  override val message: String?,
  var extraErrorTrace: String? = null
) : Exception(message) {

  @Immutable
  @Serializable
  data class ExceptionWithDiagnostics(
    override val cause: ThrowableWrapper,
    val diagnostics: List<DiagnosticsData>,
  ) : ProviderExceptions("ExceptionWithDiagnostics: ${cause.message}")

  @Immutable
  @Serializable
  data class NoMatchFound(
    val trackName: String? = null,
  ) : ProviderExceptions("$trackName : No Match Found")


  @Immutable
  @Serializable
  data object AuthNeeded : ProviderExceptions("Please Sign In")

  @Immutable
  @Serializable
  data class DownloadLinkFetchFailed(
    val trackName: String,
    val errorTrace: String? = null,
  ) : ProviderExceptions("$trackName : DownloadLinkFetch Failed", errorTrace)


  @Immutable
  @Serializable
  data class GeoLocationBlocked(
    val extraInfo: String? = null,
  ) :
    ProviderExceptions("This Content is not Accessible from your Location, try using a VPN! \nCAUSE:$extraInfo")


  @Immutable
  @Serializable
  data class LinkInvalid(
    val link: String? = null,
  ) : ProviderExceptions("Link is NOT valid.\n ${link ?: ""}")


  @Immutable
  @Serializable
  data class FeatureNotImplementedYet(
    val extraInfo: String? = null,
  ) : ProviderExceptions("Feature is not Implemented yet.", extraInfo)


  @Immutable
  @Serializable
  data class NoInternetException(
    val extraInfo: String? = null,
  ) : ProviderExceptions("Check your Internet Connectivity.", extraInfo)

  @Immutable
  @Serializable
  data class UnknownException(
    val extraInfo: String? = null,
  ) : ProviderExceptions("Unknown Exception Occurred.", extraInfo)
}
