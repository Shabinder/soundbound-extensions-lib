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
        private val LRC_TIMESTAMP_REGEX = "\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]".toRegex()
        // Matches ANY [...] pattern that is NOT a valid LRC timestamp
        // This includes [Verse 1], [Chorus], [1. Introduction], [ar: Artist], etc.
        private val NON_TIMESTAMP_BRACKET_REGEX = "\\[(?!\\d\\d:\\d\\d\\.\\d{2,3}\\])[^\\]]*\\]".toRegex()

        fun convertMillisecondsToTimeStamp(milliseconds: Long): String {
            val minutes = ((milliseconds / 1000) / 60).toString().padStart(2, '0')
            val seconds = ((milliseconds / 1000) % 60).toString().padStart(2, '0')
            val milliSeconds = ((milliseconds % 1000) / 10).toString().padStart(2, '0')
            return "${minutes}:${seconds}.${milliSeconds}"
        }

        /**
         * Validates if the given text contains actual LRC timestamps (e.g., [00:12.34])
         * and not just section markers (e.g., [Verse 1], [Chorus], etc.)
         *
         * @return true if at least one valid LRC timestamp is found in the first 10 lines
         */
        fun isValidSyncedLyrics(lyrics: String?): Boolean {
            if (lyrics.isNullOrBlank()) return false

            // Check first 10 lines for valid LRC timestamps
            val linesToCheck = lyrics.lines().take(10)
            return linesToCheck.any { line ->
                LRC_TIMESTAMP_REGEX.containsMatchIn(line)
            }
        }

        /**
         * Sanitizes plain lyrics by removing/replacing section markers like [Verse 1], [Chorus], etc.
         * This prevents plain lyrics from being confused with synced lyrics.
         *
         * @return sanitized lyrics with all non-timestamp brackets removed
         */
        fun sanitizePlainLyrics(lyrics: String?): String? {
            if (lyrics.isNullOrBlank()) return lyrics

            // If it's already valid synced lyrics, don't sanitize
            if (isValidSyncedLyrics(lyrics)) return lyrics

            // Remove ALL brackets that aren't LRC timestamps
            return lyrics.lines().joinToString("\n") { line ->
                // Replace non-timestamp brackets with their content
                NON_TIMESTAMP_BRACKET_REGEX.replace(line) { matchResult ->
                    // Extract content inside brackets and use as plain text
                    val content = matchResult.value.trim('[', ']')
                    if (content.isBlank()) "" else content
                }
            }.trimEnd()
        }
    }
}