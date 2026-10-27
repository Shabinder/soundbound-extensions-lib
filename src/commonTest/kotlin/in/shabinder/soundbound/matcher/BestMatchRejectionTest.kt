package `in`.shabinder.soundbound.matcher

import `in`.shabinder.soundbound.models.Artist
import `in`.shabinder.soundbound.zipline.FuzzySearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BestMatchRejectionTest {

    /** query title, query sec, hit title, hit sec, expected wrong-recording verdict. */
    private val cases = listOf(
        // Plain request, plain hit.
        Case("Shape of You", 234, "Shape of You", 234, false),
        Case("Blinding Lights", 200, "Blinding Lights (Official Audio)", 203, false),
        Case("Lemonade", 164, "Lemonade (Official Video)", 190, false),
        // Unrequested modifiers are rejected.
        Case("Shape of You", 234, "Shape of You (Karaoke Version)", 234, true),
        Case("Shape of You", 234, "Shape of You - Cover", 230, true),
        Case("Shape of You", 234, "Shape of You (Live at Wembley)", 250, true),
        Case("Shape of You", 234, "Shape of You [Acoustic]", 220, true),
        Case("Shape of You", 234, "Shape of You (Instrumental)", 234, true),
        Case("Shape of You", 234, "Shape of You (Slowed + Reverb)", 300, true),
        Case("Shape of You", 234, "Shape of You (Sped Up)", 180, true),
        Case("Shape of You", 234, "Shape of You Nightcore", 190, true),
        Case("Shape of You", 234, "Shape of You 8D Audio", 234, true),
        Case("Shape of You", 234, "Shape Of You (Major Lazer Remix)", 220, true),
        Case("Shape of You", 234, "Shape of You - Piano Tribute", 234, true),
        Case("Shape of You", 234, "Shape of You | Reaction", 400, true),
        Case("Kesariya", 268, "Kesariya Mashup 2022", 268, true),
        Case("Kesariya", 268, "Kesariya (Reprise)", 268, true),
        Case("Tum Hi Ho", 262, "Tum Hi Ho - Bass Boosted", 262, true),
        // The query names the word itself, so it stays allowed.
        Case("Live Forever", 276, "Live Forever", 276, false),
        Case("Cover Me", 200, "Cover Me (Remastered)", 205, false),
        Case("Zindagi Live", 210, "Zindagi Live", 212, false),
        Case("Tum Hi Ho (Live)", 300, "Tum Hi Ho Live", 300, false),
        Case("Kesariya (Remix)", 200, "Kesariya Remix by DJ", 210, false),
        // Token-based: "Alive"/"Coverage"/"Lively" do not carry "live"/"cover".
        Case("Alive", 230, "Alive", 230, false),
        Case("Lively Up Yourself", 180, "Lively Up Yourself", 180, false),
        Case("Total Coverage", 200, "Total Coverage", 200, false),
        // Indic film packaging is not a version.
        Case("Paniyon Sa", 260, "Paniyon Sa (From \"Satyamev Jayate\")", 262, false),
        Case("Jeene Laga Hoon", 230, "Jeene Laga Hoon (From \"Ramaiya Vastavaiya\")", 235, false),
        Case("Raataan Lambiyan", 230, "Raataan Lambiyan (From \"Shershaah\")", 231, false),
        // Dropped bracketed marker: the request names a version the hit lacks.
        Case("The Last Airbender (VIP)", 300, "The Last Airbender", 280, true),
        Case("Wonderwall (Acoustic)", 250, "Wonderwall", 258, true),
        Case("Africa (Live)", 300, "Africa", 295, true),
        Case("Bohemian Rhapsody (Remastered 2011)", 354, "Bohemian Rhapsody", 355, true),
        // Spelling folding: remastered == remaster, remixes == remix.
        Case("Bohemian Rhapsody (Remastered 2011)", 354, "Bohemian Rhapsody - Remaster", 355, false),
        Case("Song (Remix)", 200, "Song Remixes", 210, false),
        Case("The Last Airbender (VIP)", 300, "The Last Airbender VIP", 300, false),
        // Neutral words are not markers.
        Case("Song (Radio Edit)", 200, "Song", 210, false),
        Case("Song (Album Version)", 200, "Song", 200, false),
        // Duration ratio window [0.55, 1.8].
        Case("Song", 200, "Song", 110, false), // 0.55 exactly
        Case("Song", 200, "Song", 109, true),
        Case("Song", 200, "Song", 360, false), // 1.8 exactly
        Case("Song", 200, "Song", 361, true),
        Case("Song", 200, "Song", 3600, true), // 1-hour mix
        Case("Song", 240, "Song", 30, true), // 30 s preview
        // Skipped under 45 s and for unknown durations.
        Case("Intro", 44, "Intro", 200, false),
        Case("Song", 0, "Song", 3600, false),
        Case("Song", 200, "Song", 0, false),
        // Diacritics folded.
        Case("Café del Mar", 400, "Cafe del Mar (Live)", 400, true),
        Case("Señorita (Live)", 200, "Senorita LIVE", 200, false),
    )

    @Test
    fun table() {
        assertTrue(cases.size >= 40, "acceptance asks for 40+ cases, got ${cases.size}")
        val failures = cases.filter { c ->
            isWrongRecording(props(c.query, c.querySec), props(c.hit, c.hitSec)) != c.reject
        }
        assertEquals(emptyList(), failures)
    }

    @Test
    fun rejectedHitsAreDroppedWhenACleanOneExists() {
        val query = props("Shape of You", 234)
        val hits = listOf(props("Shape of You (Karaoke)", 234), props("Shape of You", 234))
        assertEquals(listOf("Shape of You"), orderResults(query, hits, EqualsFuzz).keys.map { it.title })
    }

    @Test
    fun fallsBackToPenaltyScoringWhenEveryHitIsRejected() {
        val query = props("Shape of You", 234)
        val hits = listOf(props("Shape of You (Karaoke)", 234), props("Shape of You", 3600))
        assertEquals(2, orderResults(query, hits, EqualsFuzz).size)
    }

    @Test
    fun cleanSearchTitle() {
        val table = mapOf(
            "Paniyon Sa (From \"Satyamev Jayate\")" to "Paniyon Sa",
            "Never Gonna Give You Up (Official Video)" to "Never Gonna Give You Up",
            "Hey Jude [Remastered 2015]" to "Hey Jude",
            "The Last Airbender (VIP)" to "The Last Airbender VIP",
            "Wonderwall (Acoustic)" to "Wonderwall Acoustic",
            "Señorita feat. Camila" to "Senorita",
            "Beyoncé  -  Halo" to "Beyonce - Halo",
            "(Original Mix)" to "(Original Mix)",
            "Kesariya" to "Kesariya",
        )
        table.forEach { (input, expected) -> assertEquals(expected, cleanSearchTitle(input), input) }
    }

    private data class Case(
        val query: String, val querySec: Long, val hit: String, val hitSec: Long, val reject: Boolean,
    )

    private fun props(name: String, sec: Long): MatchProps = object : MatchProps {
        override val title = name
        override val artists = listOf(Artist("Artist"))
        override val albumName: String? = null
        override val durationSec = sec
        override val isrc: String? = null
        override fun toString() = "$name/$sec"
    }

    /** Scores everything 100 so the test isolates the rejection/fallback step. */
    private object EqualsFuzz : FuzzySearch {
        override fun ratio(a: String, b: String) = 100f
        override fun partialRatio(a: String, b: String) = 100f
    }
}
