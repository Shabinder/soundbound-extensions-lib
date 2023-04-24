package `in`.shabinder.soundbound.models

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable

typealias DownloadRequest = Request

@Parcelize
@Serializable
class Request(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
    val method: String = HttpMethod.Get.value,
    val body: String? = null,
) : Parcelable {
    val httpMethod: HttpMethod
        get() = HttpMethod.parse(method)

    companion object {
        fun from(
            url: String,
            headers: Map<String, String> = emptyMap(),
            params: Map<String, String> = emptyMap(),
            method: String = HttpMethod.Get.value,
            body: String? = null,
        ) = Request(url, method = method, params = params, headers = headers, body = body)
    }

    suspend fun buildUsing(httpClient: HttpClient, block: HttpRequestBuilder.() -> Unit) = httpClient.buildRequest(this, block)

    override fun toString(): String {
        return "DownloadRequest(url='$url', headers=$headers, method='$method', body=$body)"
    }

    fun copy(
        url: String = this.url,
        headers: Map<String, String> = this.headers,
        method: String = this.method,
        body: String? = this.body,
        params: Map<String, String> = this.params,
    ): Request {
        return Request(
            url = url,
            headers = headers,
            method = method,
            body = body,
            params = params,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Request) return false
        if (url != other.url) return false
        if (headers != other.headers) return false
        if (method != other.method) return false
        if (body != other.body) return false
        if (params != other.params) return false
        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + (body?.hashCode() ?: 0)
        result = 31 * result + params.hashCode()
        return result
    }
}

suspend fun HttpClient.buildRequest(request: Request, block: HttpRequestBuilder.() -> Unit = {}) = prepareRequest() {
    url(request.url)
    method = request.httpMethod
    headers {
        request.headers.forEach { (key, value) ->
            append(key, value)
        }
    }
    request.params.forEach { (key, value) ->
        parameter(key, value)
    }
    if (request.body != null)
        setBody(request.body)

    block()
}