package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.diagnostics.DiagnosticContext.Key.withDiagnostics
import `in`.shabinder.soundbound.diagnostics.DiagnosticManager

import `in`.shabinder.soundbound.utils.GlobalJson
import `in`.shabinder.soundbound.utils.safeRunCatching
import kotlinx.serialization.Serializable

interface HttpClientBuilder : ZiplineService {
  fun build(enableCookies: Boolean = true): HttpClient
}

fun HttpClientBuilder.build(
  enableCookies: Boolean = true,
  config: HttpClient.() -> Unit
): HttpClient =
  build(enableCookies).apply(config)

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
    GET, POST, PUT, DELETE, HEAD
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
