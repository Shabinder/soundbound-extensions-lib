package `in`.shabinder.soundbound.providers.lyrics

import androidx.compose.runtime.Immutable
import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.QueryParams
import kotlinx.serialization.Serializable

@Immutable
interface LyricsProvider {
    // title to lyricsURL map from provider search
    suspend fun getAllLyrics(queryParams: QueryParams): Map<String, String>
    suspend fun extractLyrics(lyricsURL: String): String

    @Immutable
    @Serializable
    object LyricsNotAvailable : LyricsProvider, ZiplineService {
        override suspend fun getAllLyrics(
            queryParams: QueryParams
        ): Map<String,String> = throw IllegalArgumentException("Lyrics not available")

        override suspend fun extractLyrics(lyricsURL: String): String {
            throw IllegalArgumentException("Lyrics not available")
        }
    }
}