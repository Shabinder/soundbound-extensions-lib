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
    val body: HttpClient.BodyType = HttpClient.BodyType.NONE,
    val downloadChunkSize: Long? = null
) : Parcelable {
    val httpMethod: HttpClient.Method
        get() = HttpClient.Method.valueOf(method)

    val isDownloadToBeChunked: Boolean
        get() = downloadChunkSize != null

    companion object {
        const val DEFAULT_CHUNK_SIZE = 10485760L // 10mb
        fun from(
            url: String,
            headers: Map<String, String> = emptyMap(),
            params: Map<String, String> = emptyMap(),
            method: String = HttpClient.Method.GET.name,
            body: HttpClient.BodyType = HttpClient.BodyType.NONE,
            downloadChunkSize: Long? = null
        ) = Request(
            url,
            method = method,
            params = params,
            headers = headers,
            body = body,
            downloadChunkSize = downloadChunkSize
        )
    }

    override fun toString(): String {
        return "DownloadRequest(url='$url', headers=$headers, method='$method', body=$body, downloadChunkSize=$downloadChunkSize)"
    }

    fun copy(
        url: String = this.url,
        headers: Map<String, String> = this.headers,
        method: String = this.method,
        body: HttpClient.BodyType = this.body,
        params: Map<String, String> = this.params,
        downloadChunkSize: Long? = this.downloadChunkSize
    ): Request {
        return Request(
            url = url,
            headers = headers,
            method = method,
            body = body,
            params = params,
            downloadChunkSize = downloadChunkSize
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
        if (downloadChunkSize != other.downloadChunkSize) return false
        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + params.hashCode()
        result = 31 * result + downloadChunkSize.hashCode()
        return result
    }
}