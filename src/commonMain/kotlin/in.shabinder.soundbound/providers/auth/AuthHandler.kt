package `in`.shabinder.soundbound.providers.auth

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.providers.Dependencies
import `in`.shabinder.soundbound.providers.auth.AuthHandler.AuthMethod.*
import `in`.shabinder.soundbound.providers.auth.AuthHandler.AuthMethod.AuthData.*
import `in`.shabinder.soundbound.utils.GlobalJson
import `in`.shabinder.soundbound.utils.getSerializedOrNull
import `in`.shabinder.soundbound.utils.getSerializedOrNullFlow
import `in`.shabinder.soundbound.utils.putSerializedString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Immutable
interface AuthHandler : Dependencies {

  val authMethodType: AuthMethod
  val isAuthAvailable: Boolean
    get() = authMethodType !is AuthNotNeeded

  val needsReAuthentication: Boolean
    get() = authMethodType !is AuthNotNeeded && authStatus !is AuthStatus.Authenticated

  val authStatus: AuthStatus
    get() = when (val authType = authMethodType) {
      is CookieAuthAvailable -> {
        val saved = (mAuthData as? CookieData)?.cookies.orEmpty()

        // Both credential sources are captured into the same map (localStorage values merged
        // under their key). A cookie GROUP is satisfied if ANY key in it is present (OR within
        // a group); a localStorage key must be present. ALL declared requirements must hold,
        // and at least one must be declared — otherwise "no requirements" reads as authed.
        val cookiesSatisfied = authType.requiredCookieNames.all { group -> group.any { saved.containsKey(it.key) } }
        val localStorageSatisfied = authType.localStorageKeys.all { saved.containsKey(it.key) }
        val declaresAnyRequirement =
          authType.requiredCookieNames.isNotEmpty() || authType.localStorageKeys.isNotEmpty()

        if (declaresAnyRequirement && cookiesSatisfied && localStorageSatisfied) {
          AuthStatus.Authenticated
        } else {
          AuthStatus.NotAuthenticated
        }
      }

      is AuthNotNeeded -> AuthStatus.Authenticated
    }


  val authDataConfigKey: String

  /*protected*/
  var mAuthData: AuthData?
    set(value) {
      if (value == null) {
        devicePreferences.remove(authDataConfigKey)
      } else {
        devicePreferences.putSerializedString(authDataConfigKey, value)
      }
    }
    get() {
      if (!isAuthAvailable) {
        return NoAuthData
      }

      return devicePreferences.getSerializedOrNull<AuthData>(authDataConfigKey)
    }

  val authDataFlow: Flow<AuthData?>
    get() = devicePreferences.getSerializedOrNullFlow(authDataConfigKey, null)


  val isAuthenticated: Boolean
    get() = authStatus is AuthStatus.Authenticated

  // Subclass can override below for sanitation // cleanup if needed
  val authData: AuthData? get() = mAuthData
  fun setAuthData(authData: AuthData?) {
    mAuthData = authData
  }

  suspend fun awaitAuthData(): AuthData {
    if (!isAuthAvailable) return NoAuthData
    return authDataFlow.filterNotNull().first()
  }


  @Immutable
  @Serializable
  sealed class AuthMethod {

    // Signifies that auth is a pre-requisite for operations
    abstract val isRequired: Boolean

    @Immutable
    @Serializable
    sealed class AuthData {
      @Immutable
      @Serializable
      data class CookieData(val cookies: Map<String, String>) : AuthData()

      @Immutable
      @Serializable
      data object NoAuthData : AuthData()
    }

    @Immutable
    @Serializable
    data class CookieAuthAvailable(
      val originURL: String,
      val requiredCookieNames: List<List<CookieKey>>, // within list, any cookie name suffices, acts like OR
      val headers: Map<String, String> = emptyMap(),
      val userAgentString: String? = null,
      val saveAll: Boolean = true, // we check against required cookies, but save all.
      override val isRequired: Boolean = false,
      // Credentials some platforms keep in localStorage, not cookies (e.g. Qobuz user_auth_token
      // under `localuser`, Tidal access_token). The app evals each [LocalStorageKey.js] in the
      // logged-in page and merges the result into the same CookieData map under [LocalStorageKey.key],
      // so the extension reads it via authData.cookies[key] exactly like a cookie. Auth succeeds
      // only when all required cookies AND all localStorage keys are present.
      val localStorageKeys: List<LocalStorageKey> = emptyList(),
      // Extension-provided JS injected at document-start into the auth webview (before page JS
      // runs). Use to hook fetch/XHR and capture a token from a login response, fix layout, or
      // set up storage. Desktop wires it as the native initScript; capture results into
      // localStorage and read them back via [localStorageKeys].
      val injectedJS: String? = null,
    ) : AuthMethod() {
      @Immutable
      @Serializable
      data class CookieKey(val key: String, val forURL: String)

      @Immutable
      @Serializable
      data class LocalStorageKey(val key: String, val js: String)
    }

    @Immutable
    @Serializable
    data object AuthNotNeeded : AuthMethod() {
      override val isRequired: Boolean = false
    }
  }


  @Serializable
  @Immutable
  sealed class AuthStatus {

    @Serializable
    @Immutable
    data object Authenticated : AuthStatus()

    @Serializable
    @Immutable
    data object NotAuthenticated : AuthStatus()

    @Serializable
    @Immutable
    data class Error(val message: String) : AuthStatus()
  }
}
