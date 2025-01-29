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

  suspend fun getAsString(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): String

  suspend fun postAsString(
    url: String,
    params: Map<String, String> = emptyMap(),
    body: BodyType = BodyType.NONE,
    headers: Map<String, String> = emptyMap(),
  ): String

  suspend fun getFinalUrl(
    url: String,
    headers: Map<String, String> = emptyMap(),
  ): String

  suspend fun validateStatus(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): Boolean

  suspend fun head(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): Map<String, List<String>>

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
  getAsString(url, params, headers).let {
    if (T::class == String::class) {
      return@let it as T
    }

    safeRunCatching {
      GlobalJson.decodeFromString<T>(it)
    }.onSuccess { res ->
      appendDiagnostics(
        key = "SuccessNetworkResponse",
        value = mapOf(
          "method" to HttpClient.Method.GET.toString(),
          "url" to url,
          "params" to params.toString(),
          "headers" to headers.toString(),
          "response" to res.toString(),
        )
      )
    }.onFailure { e ->
      appendDiagnostics(
        key = "ErrorParsingResponse",
        error = e,
        extraValues = mapOf(
          "method" to HttpClient.Method.GET.toString(),
          "url" to url,
          "params" to params.toString(),
          "headers" to headers.toString(),
          "response" to it,
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
  postAsString(url, params, body, headers).let {
    if (T::class == String::class) {
      return@let it as T
    }

    safeRunCatching {
      GlobalJson.decodeFromString<T>(it)
    }.onSuccess { res ->
      appendDiagnostics(
        key = "SuccessNetworkResponse",
        value = mapOf(
          "method" to HttpClient.Method.POST.toString(),
          "url" to url,
          "params" to params.toString(),
          "headers" to headers.toString(),
          "body" to body.toString(),
          "response" to res.toString(),
        )
      )
    }.onFailure { e ->
      appendDiagnostics(
        key = "ErrorParsingResponse",
        error = e,
        extraValues = mapOf(
          "method" to HttpClient.Method.POST.toString(),
          "url" to url,
          "params" to params.toString(),
          "headers" to headers.toString(),
          "body" to body.toString(),
          "response" to it,
        )
      )
    }.getOrThrow()
  }
}
