package `in`.shabinder.soundbound.matcher

import `in`.shabinder.soundbound.models.Artist
import `in`.shabinder.soundbound.zipline.FuzzySearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** SoundBound's real misses (2026-11) and SoulSync `tests/matching/test_divergent_version.py` guard cases (MIT, see NOTICE). */
class MatchingRegressionTest {
    private data class P(
        override val title: String,
        val artist: String,
        override val durationSec: Long,
        override val isrc: String? = null,
        override val trackUrl: String? = null,
    ) : MatchProps {
        override val artists = listOf(Artist(artist))
        override val albumName: String? = null
    }

    private object NoFuzzy : FuzzySearch {
        override fun ratio(a: String, b: String) = error("unused")
        override fun partialRatio(a: String, b: String) = error("unused")
    }

    private fun accepts(name: String, artist: String, dur: Long, cand: String, candArtist: String, candDur: Long, yt: Boolean = false) =
        RecordingMatch.score(name, listOf(artist), dur, cand, listOf(candArtist), candDur, yt).accepted

    // --- real misses ---

    @Test fun bohemianRhapsodyIsrcHitThatIsStayinAliveIsRejected() {
        // YouTube's ISRC search returned a different video; the code tagged it with the wanted ISRC.
        val wanted = P("Bohemian Rhapsody", "Queen", 354, isrc = "GBUM71029604", trackUrl = "https://open.spotify.com/track/x")
        val wrong = P("Stayin' Alive", "Bee Gees", 285, isrc = "GBUM71029604", trackUrl = "https://music.youtube.com/watch?v=y")
        val right = P("Bohemian Rhapsody (Remastered 2011)", "Queen", 355, trackUrl = "https://music.youtube.com/watch?v=z")
        assertEquals(listOf(right), orderResults(wanted, listOf(wrong, right), NoFuzzy).keys.toList())
        assertTrue(orderResults(wanted, listOf(wrong), NoFuzzy).isEmpty())
        // "Alive" never reads as a live marker.
        assertTrue(RecordingMatch.versionMarkers("Stayin' Alive").isEmpty())
    }

    @Test fun shapeOfYouNeverResolvesToPerfectBySameArtist() {
        assertFalse(accepts("Shape of You", "Ed Sheeran", 234, "Perfect", "Ed Sheeran", 263))
        // Same length is not enough either (durations that happen to agree).
        assertFalse(accepts("Shape of You", "Ed Sheeran", 234, "Perfect", "Ed Sheeran", 234))
        assertTrue(accepts("Shape of You", "Ed Sheeran", 234, "Shape Of You", "Ed Sheeran", 233))
        val wanted = P("Shape of You", "Ed Sheeran", 234)
        val perfect = P("Perfect", "Ed Sheeran", 234)
        val right = P("Shape of You", "Ed Sheeran", 234)
        assertEquals(listOf(right), orderResults(wanted, listOf(perfect, right), NoFuzzy).keys.toList())
    }

    @Test fun fadedLiveAndRemixVariantsAreRejected() {
        for (variant in listOf(
            "Faded (Live)", "Faded - Live at EDC", "Faded (Restrung)", "Faded (Remix)", "Faded (Tiësto's Deep House Remix)",
            "Faded (Slowed + Reverb)", "Faded (Sped Up)", "Faded (Nightcore)", "Faded (8D Audio)", "Faded (Instrumental)",
            "Faded (Piano Version)", "Faded (Acoustic)", "Faded (Extended)", "Faded (Karaoke Version)", "Faded (Alan Walker Rmx)",
        )) {
            assertFalse(accepts("Faded", "Alan Walker", 213, variant, "Alan Walker", 213, yt = true), variant)
            assertFalse(accepts(variant, "Alan Walker", 213, "Faded", "Alan Walker", 213, yt = true), "reverse $variant")
        }
        assertTrue(accepts("Faded", "Alan Walker", 213, "Faded (Official Music Video)", "Alan Walker", 220, yt = true))
        assertTrue(accepts("Faded (Restrung)", "Alan Walker", 200, "Faded - Restrung", "Alan Walker", 200))
        assertFalse(accepts("Faded", "Alan Walker", 213, "Faded", "Alan Walker Tribute Band", 213))
        assertFalse(accepts("Faded", "Alan Walker", 213, "Faded", "Karaoke Hits Channel", 213))
    }

    // --- SoulSync divergent-version / derivative-artist guards ---

    @Test fun soulSyncDivergentVersionsAreRejected() {
        val a = "Empire Of The Sun"
        listOf(
            "We Are The People (Shazam Remix)" to "We Are The People (southstar Remix)",
            "We Are The People (Shazam Remix)" to "We Are The People (ARTBAT Remix)",
            "All Night (Live @ Pukkelpop)" to "All Night (Umek Remix)",
            "All Night (Live at Pukkelpop)" to "All Night (Live at Wembley)",
            "Emerge (Junkie XL Remix)" to "Emerge (DFA Version)",
            "Song Title (Remix)" to "Song Title (Live)",
        ).forEach { (w, c) -> assertFalse(accepts(w, a, 0, c, a, 0), "$w vs $c") }
        assertTrue(accepts("We Are The People (Shazam Remix)", a, 344, "We Are The People (Shazam Remix)", a, 344))
    }

    @Test fun soulSyncSameVersionDifferentWrappingMatches() {
        listOf("Song (Live)" to "Song - Live", "Hey Jude (Acoustic)" to "Hey Jude - Acoustic", "Track (Unplugged)" to "Track - Unplugged")
            .forEach { (a, b) -> assertTrue(accepts(a, "The Beatles", 431, b, "The Beatles", 431), "$a vs $b") }
        assertFalse(accepts("Song (Live)", "X", 200, "Song - Acoustic", "X", 200))
        assertTrue(accepts("Song Title", "X", 200, "Song Title (2011 Remaster)", "X", 200))
    }

    @Test fun soulSyncDerivativeArtistsAreRejected() {
        for (cand in listOf(
            "Metallica Tribute Band", "The Metallica Cover Band", "Metallica Salute", "Metallica Parody",
            "Metallica Soundalike", "Metallica Backing Track",
        )) assertFalse(accepts("Enter Sandman", "Metallica", 331, "Enter Sandman", cand, 331), cand)
        // The source itself is the orchestra: same credit, same recording.
        assertTrue(accepts("Enter Sandman", "Vitamin String Quartet Tribute", 300, "Enter Sandman", "Vitamin String Quartet Tribute", 300))
    }

    // --- confidence / reason API, ID guard, romanised variants ---

    @Test fun verdictCarriesReasonAndConfidence() {
        val strong = RecordingMatch.score("Faded", listOf("Alan Walker"), 213, "Faded", listOf("Alan Walker"), 213)
        assertEquals(MatchReason.STRICT_TITLE_AND_DURATION, strong.reason)
        assertEquals(MatchConfidence.STRONG, strong.confidence)
        val wide = RecordingMatch.score("Faded", listOf("Alan Walker"), 213, "Faded", listOf("Alan Walker"), 223, youtube = true)
        assertEquals(MatchReason.STRICT_TITLE, wide.reason)
        assertEquals(MatchConfidence.LOOSE, wide.confidence)
        val loose = RecordingMatch.score("Tri", listOf("Popeye Bangladesh"), 241, "Popeye (Bangladesh) - Tri (ত্রি) Official Music Video", listOf("Popeye Bangladesh"), 241)
        assertEquals(MatchReason.LOOSE_TITLE_AND_DURATION, loose.reason)
        assertEquals(MatchReason.VERSION_MISMATCH, RecordingMatch.score("Faded", listOf("A"), 1, "Faded (Live)", listOf("A"), 1).reason)
        assertEquals(MatchReason.FEATURED_GUEST_MISMATCH, RecordingMatch.score("A (feat. B)", listOf("A"), 1, "A", listOf("A"), 1).reason)
        assertEquals(MatchReason.ARTIST_MISMATCH, RecordingMatch.score("Faded", listOf("Alan Walker"), 213, "Faded", listOf("Karaoke Hits"), 213).reason)
        assertEquals(MatchReason.DURATION_MISMATCH, RecordingMatch.score("Faded", listOf("A"), 200, "Faded", listOf("A"), 260).reason)
        assertEquals(MatchReason.TITLE_MISMATCH, RecordingMatch.score("Shape of You", listOf("Ed Sheeran"), 234, "Perfect", listOf("Ed Sheeran"), 234).reason)
        // Old two-arg construction still works (ABI/source compatibility).
        assertEquals(MatchConfidence.LOOSE, MatchVerdict(50f, true).confidence)
        assertEquals(MatchConfidence.NONE, MatchVerdict(0f, false).confidence)
    }

    @Test fun rankMatchesReportsIsrcAndFallbackReasons() {
        val wanted = P("A Song", "Artist", 180, isrc = "USABC2600001")
        val byIsrc = P("A Song (2011 Remaster)", "Artist", 181, isrc = "US-ABC-26-00001")
        assertEquals(MatchReason.ISRC, rankMatches(wanted, listOf(byIsrc)).single().second.reason)
        // Missing metadata never disproves the code (no duration, no artists).
        val bare = object : MatchProps {
            override val title = "A Song"; override val artists = emptyList<Artist>(); override val albumName: String? = null
            override val durationSec = 0L; override val isrc = "USABC2600001"
        }
        assertEquals(MatchReason.ISRC, rankMatches(wanted, listOf(bare)).single().second.reason)
        // songmirror test_isrc_fallback_does_not_ignore_an_explicit_recording_conflict: same code, different cut.
        val studio = P("A Song", "Artist", 180, isrc = "USABC2600001")
        for (q in listOf("Acoustic", "Dub", "feat. Guest", "First DJ Remix")) {
            val variant = P("A Song ($q)", "Artist", 180, isrc = "USABC2600001")
            assertTrue(rankMatches(wanted, listOf(variant)).none { it.second.reason == MatchReason.ISRC }, q)
            assertTrue(rankMatches(wanted, listOf(variant)).none { it.second.confidence >= MatchConfidence.STRONG }, q)
            assertEquals(listOf(studio to MatchReason.ISRC), rankMatches(wanted, listOf(variant, studio)).map { it.first to it.second.reason }, q)
        }
        // songmirror test_isrc_cache_cannot_override_conflicting_recording_metadata: performer / length.
        assertTrue(rankMatches(wanted, listOf(P("A Song", "Different Performer", 180, isrc = "USABC2600001"))).isEmpty())
        assertTrue(rankMatches(wanted, listOf(P("A Song", "Artist", 230, isrc = "USABC2600001"))).isEmpty())
        val live = P("Pasoori (Live)", "Ali Sethi", 224)
        assertEquals(MatchReason.SAME_LABEL_FALLBACK, rankMatches(P("Pasoori", "Ali Sethi", 224), listOf(live)).single().second.reason)
    }

    @Test fun romanisedVariantsMatchAcrossScripts() {
        assertEquals("kamin", RecordingMatch.romanize(RecordingMatch.normalizeText("Камин")))
        assertTrue(accepts("Камин", "Emin", 200, "Kamin", "Emin", 200))
        assertTrue(accepts("Kamin", "Эмин", 200, "Kamin", "Emin", 200))
        assertEquals(setOf("live"), RecordingMatch.versionMarkers("Камин (Live)"))
        assertFalse(accepts("Камин", "Emin", 200, "Kamin (Live)", "Emin", 200))
        assertFalse(accepts("Камин", "Emin", 200, "Kamin", "Эмин Трибьют", 200)) // different credit text entirely
    }
}
