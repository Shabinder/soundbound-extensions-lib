package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.parcelize.Parcelable
import `in`.shabinder.soundbound.parcelize.Parcelize
import `in`.shabinder.soundbound.zipline.HttpClient
import kotlinx.serialization.Serializable

typealias DownloadRequest = Request

@Parcelize
@Immutable
@Serializable
class Request(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
    val method: String = HttpClient.Method.GET.name,
    val body: String? = null,
) : Parcelable {
    val httpMethod: HttpClient.Method
        get() = HttpClient.Method.valueOf(method)

    companion object {
        fun from(
            url: String,
            headers: Map<String, String> = emptyMap(),
            params: Map<String, String> = emptyMap(),
            method: String = HttpClient.Method.GET.name,
            body: String? = null,
        ) = Request(url, method = method, params = params, headers = headers, body = body)
    }

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