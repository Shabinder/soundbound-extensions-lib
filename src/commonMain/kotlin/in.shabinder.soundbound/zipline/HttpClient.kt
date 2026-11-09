package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.diagnostics.DiagnosticContext.Key.withDiagnostics
import `in`.shabinder.soundbound.diagnostics.DiagnosticManager

import `in`.shabinder.soundbound.utils.GlobalJson
import `in`.shabinder.soundbound.utils.safeRunCatching
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

@Serializable
data class BinaryHttpResponse(
  val statusCode: Int,
  val headers: Map<String, List<String>>,
  val responseUrl: String,
  @Contextual val body: ByteArray,
) {
  fun isSuccess(): Boolean = statusCode in 200..299
}

interface HttpClientBuilder : ZiplineService {
  fun build(enableCookies: Boolean = true): HttpClient
}

fun HttpClientBuilder.build(
  enableCookies: Boolean = true,
  config: HttpClient.() -> Unit
): HttpClient =
  build(enableCookies).apply(config)

/**
 * Opts requests in to host-side bot-challenge solving. Some sites answer plain HTTP clients with an
 * interstitial "checking your browser" page (HTTP 403/503) that only a real browser engine can pass.
 * A request carrying this header lets the host pass it in a system web view, visibly to the user
 * when it needs interaction, then retry the request with the clearance it earned. The extension
 * receives only the final response; the clearance cookies and the matching user agent stay in the
 * host, which keeps sending them for that site. The host strips the header before sending.
 *
 * Send it per request, or on every request of a client via [enableChallengeSolving]. Hosts that
 * predate it ignore it, and the user can switch solving off in the host.
 */
const val SOLVE_CHALLENGE_HEADER = "X-Soundbound-Solve-Challenge"

/** Adds [SOLVE_CHALLENGE_HEADER] to this client's default headers, so every request opts in. */
fun HttpClient.enableChallengeSolving() = setDefaultHeaders(defaultHeaders() + (SOLVE_CHALLENGE_HEADER to "1"))

@Serializable
data class CustomHttpResponse(
  val body: String,
  val headers: Map<String, List<String>>,
  val statusCode: Int,
  val responseUrl: String // Useful for debugging or if redirects happened
) {
  fun isSuccess(): Boolean {
    return statusCode in 200..299
  }
}

interface HttpClient : ZiplineService {

  var isCookiesEnabled: Boolean

  // Very Basic Cookie Support
  // Alive for the session.
  @Serializable
  data class Cookie(
    val url: String,
    val value: String,
  )

  fun getCookies(): Map<String, Cookie>
  fun setCookie(name: String, cookie: Cookie)
  fun clearCookies()

  fun getAuthInfo(): AuthType
  fun setAuth(auth: AuthType)

  fun defaultHeaders(): Map<String, String>

  fun setDefaultHeaders(headers: Map<String, String>)

  fun getDefaultURL(): String? = null
  fun setDefaultURL(url: String?)

  fun getDefaultParams(): Map<String, String>
  fun setDefaultParams(params: Map<String, String>)

  suspend fun getRequest(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): CustomHttpResponse

  suspend fun postRequest(
    url: String,
    params: Map<String, String> = emptyMap(),
    body: BodyType = BodyType.NONE,
    headers: Map<String, String> = emptyMap(),
  ): CustomHttpResponse

  /** Performs a binary request without encoding either payload as text. */
  suspend fun postBytes(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    body: ByteArray,
  ): BinaryHttpResponse

  /**
   * Any-method request (PUT, DELETE, PATCH, ...), for REST APIs whose writes need them.
   * Default covers GET/POST/HEAD through the methods above, so hosts that predate it keep working
   * for those; other methods need a host that overrides this (older hosts throw
   * `ZiplineApiMismatchException` across the bridge, which callers should treat as "unsupported").
   */
  suspend fun request(
    method: Method,
    url: String,
    params: Map<String, String> = emptyMap(),
    body: BodyType = BodyType.NONE,
    headers: Map<String, String> = emptyMap(),
  ): CustomHttpResponse = when (method) {
    Method.GET -> getRequest(url, params, headers)
    Method.POST -> postRequest(url, params, body, headers)
    Method.HEAD -> headRequest(url, params, headers)
    else -> throw UnsupportedOperationException("HTTP $method not supported by this host")
  }

  suspend fun headRequest(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): CustomHttpResponse

  suspend fun getAsString(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): String = withDiagnostics {
    getRequest(url, params, headers)
      .also {
        appendDiagnostics("GetRequest", mapOf(
          "curl" to DiagnosticManager.buildCurlCommand(
            method = "GET",
            url = url,
            headers = headers,
            params = params
          ),
          "responseCode" to it.statusCode.toString(),
          "responseUrl" to it.responseUrl,
          "responseHeaders" to it.headers.toString(),
          "responseBody" to it.body
        ))
      }
      .body
  }

  suspend fun postAsString(
    url: String,
    params: Map<String, String> = emptyMap(),
    body: BodyType = BodyType.NONE,
    headers: Map<String, String> = emptyMap(),
  ): String = withDiagnostics {
    postRequest(url, params, body, headers)
      .also {
        appendDiagnostics("PostRequest", mapOf(
          "curl" to DiagnosticManager.buildCurlCommand(
            method = "POST",
            url = url,
            headers = headers,
            params = params,
            body = body.toBodyString(),
            bodyType = body.typeName()
          ),
          "responseCode" to it.statusCode.toString(),
          "responseUrl" to it.responseUrl,
          "responseHeaders" to it.headers.toString(),
          "responseBody" to it.body,
        ))
      }
      .body
  }

  suspend fun getFinalUrl(
    url: String,
    headers: Map<String, String> = emptyMap(),
  ): String

  suspend fun validateStatus(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): Boolean

  @Serializable
  sealed interface AuthType {
    @Serializable
    data object NONE : AuthType

    @Serializable
    data class BASIC(
      val username: String,
      val password: String,
    ) : AuthType

    @Serializable
    data class BEARER(
      val token: String,
    ) : AuthType
  }

  @Serializable

  sealed interface BodyType {
    @Serializable

    data object NONE : BodyType


    @Serializable
    data class JSON(
      val json: String,
    ) : BodyType


    @Serializable
    data class FORM(
      val form: Map<String, String>,
    ) : BodyType


    @Serializable
    data class RAW(
      val raw: String,
    ) : BodyType
  }


  @Serializable
  enum class Method {
    // PATCH (lib 2026-11): a host built before it cannot decode the name, so that call fails; callers may fall back.
    GET, POST, PUT, DELETE, HEAD, PATCH
  }
}

/**
 * Extracts the body content as a string for diagnostic/curl purposes.
 */
fun HttpClient.BodyType.toBodyString(): String? = when (this) {
  is HttpClient.BodyType.NONE -> null
  is HttpClient.BodyType.JSON -> json
  is HttpClient.BodyType.FORM -> form.entries.joinToString("&") { (k, v) -> "$k=$v" }
  is HttpClient.BodyType.RAW -> raw
}

/**
 * Returns the body type name for diagnostic/curl purposes.
 */
fun HttpClient.BodyType.typeName(): String = when (this) {
  is HttpClient.BodyType.NONE -> "NONE"
  is HttpClient.BodyType.JSON -> "JSON"
  is HttpClient.BodyType.FORM -> "FORM"
  is HttpClient.BodyType.RAW -> "RAW"
}

suspend inline fun <reified T> HttpClient.get(
  url: String,
  params: Map<String, String> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
): T = withDiagnostics {
  val result = getRequest(url, params, headers)

  result.body.let {
    if (T::class == String::class) {
      return@let it as T
    }

    safeRunCatching {
      GlobalJson.decodeFromString<T>(it)
    }.onFailure { e ->
      appendDiagnostics(
        key = "ErrorParsingResponse",
        error = e,
        extraValues = mapOf(
          "curl" to DiagnosticManager.buildCurlCommand(
            method = "GET",
            url = url,
            headers = headers,
            params = params
          ),
          "responseCode" to result.statusCode.toString(),
          "responseHeaders" to result.headers.toString(),
          "responseBody" to it,
        )
      )
    }.getOrThrow()
  }
}

suspend inline fun <reified T> HttpClient.post(
  url: String,
  params: Map<String, String> = emptyMap(),
  body: HttpClient.BodyType = HttpClient.BodyType.NONE,
  headers: Map<String, String> = emptyMap(),
): T = withDiagnostics {
  val result = postRequest(url, params, body, headers)

  result.body.let {
    if (T::class == String::class) {
      return@let it as T
    }

    safeRunCatching {
      GlobalJson.decodeFromString<T>(it)
    }.onFailure { e ->
      appendDiagnostics(
        key = "ErrorParsingResponse",
        error = e,
        extraValues = mapOf(
          "curl" to DiagnosticManager.buildCurlCommand(
            method = "POST",
            url = url,
            headers = headers,
            params = params,
            body = body.toBodyString(),
            bodyType = body.typeName()
          ),
          "responseCode" to result.statusCode.toString(),
          "responseHeaders" to result.headers.toString(),
          "responseBody" to it,
        )
      )
    }.getOrThrow()
  }
}
