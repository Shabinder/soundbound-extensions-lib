package `in`.shabinder.soundbound.utils

import `in`.shabinder.soundbound.models.Artist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmName
import kotlin.math.min


@Suppress("NOTHING_TO_INLINE")
internal inline fun List<String>.cleaned(): List<String> {
    return this.filter { it.isNotBlank() && it != "null" }
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("cleanedArtists")
internal inline fun List<Artist>.cleaned(): List<Artist> {
    return filter { it.name.isNotBlank() && it.name != "null" }
}

// Not the Best Practice, use when you just care less for the exceptions.
inline fun <R> safeRunCatching(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (t: TimeoutCancellationException) {
        Result.failure(t)
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
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