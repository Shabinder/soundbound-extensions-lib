package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService


import `in`.shabinder.soundbound.utils.GlobalJson
import kotlinx.serialization.Serializable

interface HttpClientBuilder : ZiplineService {
     fun build(): HttpClient
}

fun HttpClientBuilder.build(config: HttpClient.() -> Unit): HttpClient =
    build().apply(config)

interface HttpClient: ZiplineService {

    var isCookiesEnabled: Boolean

    fun getCookies(): Map<String, String>
    fun setCookie(url: String, cookie: String)

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
): T {
    return getAsString(url, params, headers).let {
        if (T::class == String::class) {
            return@let it as T
        }

        GlobalJson.decodeFromString(it)
    }
}

suspend inline fun <reified T> HttpClient.post(
    url: String,
    params: Map<String, String> = emptyMap(),
    body: HttpClient.BodyType = HttpClient.BodyType.NONE,
    headers: Map<String, String> = emptyMap(),
): T {
    return postAsString(url, params, body, headers).let {
        if (T::class == String::class) {
            return@let it as T
        }

        GlobalJson.decodeFromString(it)
    }
}