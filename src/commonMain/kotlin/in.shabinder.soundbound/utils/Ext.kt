package `in`.shabinder.soundbound.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.math.min


@Suppress("NOTHING_TO_INLINE")
internal inline fun List<String>.cleaned(): List<String> {
    return this.filter { it.isNotBlank() && it != "null" }
}

fun <T> T.limitDecimals(maxDecimals: Int): String {
    val result = toString()
    val lastIndex = result.length - 1
    var pos = lastIndex
    while (pos >= 0 && result[pos] != '.') {
        pos--
    }
    return if (maxDecimals < 1 && pos >= 0) {
        result.substring(0, min(pos, result.length))
    } else if (pos >= 0) {
        result.substring(0, min(pos + 1 + maxDecimals, result.length))
    } else {
        return result
    }
}

val GlobalJson by lazy {
    Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true

        @OptIn(ExperimentalSerializationApi::class)
        explicitNulls = false
    }
}