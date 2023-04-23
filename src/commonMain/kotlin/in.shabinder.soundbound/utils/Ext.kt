package `in`.shabinder.soundbound.utils

import io.ktor.client.*
import io.ktor.client.request.*

/*
* Return URL after Redirections
*   - If Fails returns Input Url
* */
suspend inline fun HttpClient.getFinalUrl(
    url: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): String {
    return runCatching {
        get(url, block).call.request.url.toString()
    }.getOrNull() ?: url
}

internal fun List<String>.cleaned(): List<String> {
    return this.filter { it.isNotBlank() && it != "null" }
}
