package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import `in`.shabinder.soundbound.zipline.HttpClient
import kotlinx.serialization.Serializable

typealias DownloadRequest = Request


@Immutable
@Serializable
class Request(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
    val method: String = HttpClient.Method.GET.name,
    val body: HttpClient.BodyType = HttpClient.BodyType.NONE,
    val downloadChunkSize: Long? = null,
    val decryption: DecryptionSpec? = null,
    // DASH/segmented streams (e.g. Tidal): ordered media-segment URLs the app downloads and
    // concatenates (after [initSegment]) into one fragmented-MP4, then remuxes to the target
    // container. Non-null routes the download through the segment-assembly path. The bytes
    // never cross the extension boundary — the extension only resolves the URL list.
    val segments: List<String>? = null,
    val initSegment: String? = null,
) {
    val httpMethod: HttpClient.Method
        get() = HttpClient.Method.valueOf(method)

    val isDownloadToBeChunked: Boolean
        get() = downloadChunkSize != null

    val isSegmented: Boolean
        get() = !segments.isNullOrEmpty()

    companion object {
        const val DEFAULT_CHUNK_SIZE = 10485760L // 10mb
        fun from(
            url: String,
            headers: Map<String, String> = emptyMap(),
            params: Map<String, String> = emptyMap(),
            method: String = HttpClient.Method.GET.name,
            body: HttpClient.BodyType = HttpClient.BodyType.NONE,
            downloadChunkSize: Long? = null,
            decryption: DecryptionSpec? = null,
            segments: List<String>? = null,
            initSegment: String? = null,
        ) = Request(
            url,
            method = method,
            params = params,
            headers = headers,
            body = body,
            downloadChunkSize = downloadChunkSize,
            decryption = decryption,
            segments = segments,
            initSegment = initSegment,
        )
    }

    override fun toString(): String {
        return "DownloadRequest(url='$url', headers=$headers, method='$method', body=$body, downloadChunkSize=$downloadChunkSize, decryption=${decryption?.algorithm}, segments=${segments?.size})"
    }

    fun copy(
        url: String = this.url,
        headers: Map<String, String> = this.headers,
        method: String = this.method,
        body: HttpClient.BodyType = this.body,
        params: Map<String, String> = this.params,
        downloadChunkSize: Long? = this.downloadChunkSize,
        decryption: DecryptionSpec? = this.decryption,
        segments: List<String>? = this.segments,
        initSegment: String? = this.initSegment,
    ): Request {
        return Request(
            url = url,
            headers = headers,
            method = method,
            body = body,
            params = params,
            downloadChunkSize = downloadChunkSize,
            decryption = decryption,
            segments = segments,
            initSegment = initSegment,
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
        if (decryption != other.decryption) return false
        if (segments != other.segments) return false
        if (initSegment != other.initSegment) return false
        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + params.hashCode()
        result = 31 * result + downloadChunkSize.hashCode()
        result = 31 * result + (decryption?.hashCode() ?: 0)
        result = 31 * result + (segments?.hashCode() ?: 0)
        result = 31 * result + (initSegment?.hashCode() ?: 0)
        return result
    }
}