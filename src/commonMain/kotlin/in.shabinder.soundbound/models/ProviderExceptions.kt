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

  /**
   * Signed in, but the account's plan has no access to the requested feature (Apple Music error 40015
   * "CloudLibrary": the library API needs an active subscription). Not a sign-in problem: show
   * "[service] library needs an active subscription" instead of a sign-in banner.
   * Like [AuthNeeded], hosts match it across the Zipline bridge by class name ("SubscriptionRequired").
   */
  @Immutable
  @Serializable
  data class SubscriptionRequired(
    val service: String? = null,
    val extraInfo: String? = null,
  ) : ProviderExceptions("SubscriptionRequired: ${service ?: "This"} library needs an active subscription", extraInfo)

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
