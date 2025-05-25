package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.diagnostics.DiagnosticContext.Key.withDiagnostics


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
          "url" to url,
          "params" to params.toString(),
          "headers" to headers.toString(),
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
          "url" to url,
          "params" to params.toString(),
          "headers" to headers.toString(),
          "body" to body.toString(),
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
          "method" to HttpClient.Method.GET.toString(),
          "url" to url,
          "params" to params.toString(),
          "headers" to headers.toString(),
          "responseHeaders" to result.headers.toString(),
          "responseCode" to result.statusCode.toString(),
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
          "method" to HttpClient.Method.POST.toString(),
          "url" to url,
          "body" to body.toString(),
          "params" to params.toString(),
          "headers" to headers.toString(),
          "responseHeaders" to result.headers.toString(),
          "responseCode" to result.statusCode.toString(),
          "responseBody" to it,
        )
      )
    }.getOrThrow()
  }
}
