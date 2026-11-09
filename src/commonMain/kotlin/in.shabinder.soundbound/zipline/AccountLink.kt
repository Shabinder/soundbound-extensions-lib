package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

/**
 * What an extension needs to link a third-party account with OAuth2 + PKCE when the client secret lives on the
 * SoundBound backend: open the provider's consent page and capture its redirect, call an allowlisted backend function
 * (the host adds the signed-in user's session), keep the resulting tokens in the platform keystore, and draw secure
 * random bytes for the PKCE verifier and `state`.
 *
 * The host scopes secrets per extension, accepts only loopback `http://127.0.0.1[:port]/...` or a custom-scheme
 * redirect it has registered, and only backend functions it allowlists. Older hosts don't bind this; the guest then
 * sees [isAvailable] false.
 */
interface AccountLink : ZiplineService {
  suspend fun isAvailable(): Boolean

  /**
   * Opens [url] in the system browser (or the platform's auth session) and waits until it is redirected to
   * [redirectUri]. Returns that full redirect URL (with `code`/`state`/`error` query), or null when the user cancelled,
   * it timed out, or this host can't capture [redirectUri].
   */
  suspend fun authorize(url: String, redirectUri: String): String?

  /**
   * The first of [candidates] (redirect URIs the provider accepts, best first) this host can capture right now, e.g.
   * a custom scheme only when the installed app is registered for it, else a loopback URI. Null = none.
   * Hosts before this member existed throw across the bridge; callers then fall back to their own choice.
   */
  suspend fun pickRedirect(candidates: List<String>): String? = candidates.firstOrNull()

  /**
   * POST [jsonBody] to the SoundBound backend function [name] as the signed-in user. Never throws for an HTTP error:
   * the status comes back in [CustomHttpResponse.statusCode] (401 = not signed in to SoundBound, 0 = network failure).
   */
  suspend fun callBackend(name: String, jsonBody: String): CustomHttpResponse

  suspend fun getSecret(key: String): String?
  suspend fun putSecret(key: String, value: String)
  suspend fun removeSecret(key: String)

  /** [byteCount] bytes from a CSPRNG, base64url without padding. */
  suspend fun randomUrlSafe(byteCount: Int): String
}

/** Used by hosts without account linking (tests, old hosts). */
object UnavailableAccountLink : AccountLink {
  override suspend fun isAvailable(): Boolean = false
  override suspend fun authorize(url: String, redirectUri: String): String? = null
  override suspend fun pickRedirect(candidates: List<String>): String? = null
  override suspend fun callBackend(name: String, jsonBody: String) = CustomHttpResponse("", emptyMap(), 0, "")
  override suspend fun getSecret(key: String): String? = null
  override suspend fun putSecret(key: String, value: String) = throw UnsupportedOperationException("no account link")
  override suspend fun removeSecret(key: String) = Unit
  override suspend fun randomUrlSafe(byteCount: Int): String = throw UnsupportedOperationException("no account link")
}
