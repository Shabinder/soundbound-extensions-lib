package `in`.shabinder.soundbound.providers.lyrics

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.QueryParams

@Immutable
interface LyricsProvider {
    val isSyncedLyricsSupported: Boolean get() = false

    // title to lyricsURL map from provider search
    suspend fun getAllLyrics(queryParams: QueryParams): Map<String, String>
    suspend fun extractLyrics(lyricsURL: String): String

    @Immutable
    interface LyricsNotAvailable : LyricsProvider {
        override suspend fun getAllLyrics(
            queryParams: QueryParams
        ): Map<String, String> = emptyMap()

        override suspend fun extractLyrics(lyricsURL: String): String {
            throw IllegalArgumentException("Lyrics not available")
        }
    }

    companion object {
        fun convertMillisecondsToTimeStamp(milliseconds: Long): String {
            val minutes = ((milliseconds / 1000) / 60).toString().padStart(2, '0')
            val seconds = ((milliseconds / 1000) % 60).toString().padStart(2, '0')
            val milliSeconds = ((milliseconds % 1000) / 10).toString().padStart(2, '0')
            return "${minutes}:${seconds}.${milliSeconds}"
        }
    }
}