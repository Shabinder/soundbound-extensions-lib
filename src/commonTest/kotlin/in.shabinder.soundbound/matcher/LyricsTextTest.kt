package `in`.shabinder.soundbound.matcher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LyricsTextTest {
    @Test
    fun versionTagsAreWordBounded() {
        assertEquals(setOf("live"), LyricsText.versionTags("Kesariya (Live)"))
        assertEquals(emptySet(), LyricsText.versionTags("Deliver Us"))
        assertEquals(setOf("mandarin"), LyricsText.versionTags("Despacito 缓缓 (Mandarin Version)"))
        assertTrue(LyricsText.versionConflict("Faded", "Faded (Remix)"))
        assertFalse(LyricsText.versionConflict("Faded", "FADED"))
    }

    @Test
    fun lrcTimesInEveryFractionWidth() {
        fun ms(tag: String) = LyricsText.lrcMillis(LyricsText.LRC_TIME_TAG.find(tag)!!)
        assertEquals(83_000, ms("[1:23]"))
        assertEquals(83_400, ms("[01:23.4]"))
        assertEquals(83_450, ms("[01:23.45]"))
        assertEquals(83_456, ms("[01:23.456]"))
        assertEquals(83_450, ms("[01:23:45]"))
        assertEquals(1_500, LyricsText.lrcMillis(LyricsText.LRC_WORD_TAG.find("<00:01.50>")!!))
        assertEquals(212_990, LyricsText.lastLrcMillis("[00:01.00]a\n[03:32.99]b\n[02:00]c"))
        assertNull(LyricsText.lastLrcMillis("plain text"))
    }
}
