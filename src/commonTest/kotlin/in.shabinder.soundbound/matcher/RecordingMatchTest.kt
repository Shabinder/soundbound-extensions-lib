package `in`.shabinder.soundbound.matcher

import `in`.shabinder.soundbound.models.Artist
import `in`.shabinder.soundbound.zipline.FuzzySearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ported from songmirror `tests/test_recording_versions.py` and `tests/test_matching.py` (MIT, see NOTICE), plus
 * fold-set fixtures from Docs/RESEARCH/song-identity-benchmark (real store titles/credits/durations, 2026-11-07).
 * Durations are seconds (songmirror uses ms).
 */
class RecordingMatchTest {
    private fun accepts(
        name: String, artists: List<String>, dur: Long, cand: String, candArtist: String, candDur: Long, yt: Boolean = false,
    ) = RecordingMatch.score(name, artists, dur, cand, listOf(candArtist), candDur, yt).accepted

    private fun accepts(name: String, artist: String, dur: Long, cand: String, candArtist: String, candDur: Long, yt: Boolean = false) =
        accepts(name, listOf(artist), dur, cand, candArtist, candDur, yt)

    // --- test_recording_versions.py ---

    @Test fun rejectsConflictingPerformerOrRecordingLength() {
        assertFalse(accepts("A Song", "Black Pistol Fire", 229, "A Song", "Black Math", 208))
        assertFalse(accepts("A Song", "Black Pistol Fire", 229, "A Song", "Black Math", 229))
        assertFalse(accepts("A Song", "AURORA", 249, "A Song", "AURORA Tribute Band", 249))
        assertFalse(accepts("A Song", "Artist", 180, "A Song", "Artist", 230))
    }

    @Test fun sharedGuestCannotHideDifferentPrimaryPerformer() {
        assertFalse(accepts("A Song", listOf("Original Artist", "Shared Guest"), 180, "A Song", "Different Artist, Shared Guest", 180))
    }

    @Test fun explicitCreditSeparatorsAllowProvidersPrimaryCredit() {
        for (sep in listOf("ft.", "with", "feat.", "&", ",")) {
            assertTrue(accepts("A Song", "Original Artist $sep Guest", 180, "A Song", "Original Artist", 180), sep)
        }
    }

    @Test fun studioAndAcousticNeverMatch() {
        for (label in listOf("Acoustic", "Akustik", "Acústico", "Acoustique", "Unplugged", "Stripped")) {
            assertFalse(accepts("A Song", "Artist", 180, "A Song ($label)", "Artist", 180), label)
            assertFalse(accepts("A Song ($label)", "Artist", 180, "A Song", "Artist", 180), "reverse $label")
        }
    }

    @Test fun studioAndOtherVariantsNeverMatch() {
        val labels = listOf(
            "Dub", "Club Mix", "Extended Mix", "Radio Edit", "Single Edit", "VIP", "A Cappella", "Orchestral",
            "Re-recorded", "Slowed + Reverb", "Nightcore", "Instrumental", "Demo", "Karaoke", "Cover", "Solo Version",
            "Remix",
            // SoundBound additions (benchmark brief): sped up, 8D, lofi, live.
            "Sped Up", "8D Audio", "Lofi", "Live",
        )
        for (label in labels) {
            assertFalse(accepts("A Song", "Artist", 180, "A Song ($label)", "Artist", 180), label)
            assertFalse(accepts("A Song ($label)", "Artist", 180, "A Song", "Artist", 180), "reverse $label")
        }
    }

    @Test fun differentVariantsWithinSameFamilyDoNotMatch() {
        listOf(
            "A Song (First DJ Remix)" to "A Song (Second DJ Remix)",
            "A Song (Live at Wembley)" to "A Song (Live at Glastonbury)",
            "A Song (Version 1)" to "A Song (Version 2)",
            "A Song (Radio Edit)" to "A Song (Single Edit)",
        ).forEach { (a, b) -> assertFalse(accepts(a, "Artist", 180, b, "Artist", 180), "$a vs $b") }
    }

    @Test fun featuredCreditAndVariantSpellingMustDescribeSameRecording() {
        data class C(val src: String, val srcArtists: List<String>, val cand: String, val candArtist: String, val expected: Boolean)
        listOf(
            C("A Song", listOf("Artist"), "A Song (feat. Guest)", "Artist", false),
            C("A Song (feat. Guest)", listOf("Artist"), "A Song", "Artist", false),
            C("A Song (feat. First Guest)", listOf("Artist"), "A Song (feat. Second Guest)", "Artist", false),
            C("A Song (feat. Guest)", listOf("Artist"), "A Song", "Artist, Guest", true),
            C("A Song", listOf("Artist", "Guest"), "A Song (feat. Guest)", "Artist", true),
            C("A Song feat. Guest (Acoustic)", listOf("Artist"), "A Song (Acoustic) [ft. Guest]", "Artist", true),
            C("A Song (feat. Live)", listOf("Artist"), "A Song", "Artist, Live", true),
            C("A Song (Acoustic Version)", listOf("Artist"), "A Song - Akustik", "Artist", true),
        ).forEach { c ->
            assertEquals(c.expected, accepts(c.src, c.srcArtists, 180, c.cand, c.candArtist, 180), "${c.src} vs ${c.cand}")
        }
    }

    @Test fun deezerStyleSeparateVersionFieldIsVisible() {
        // Deezer title + title_version joined by the provider ("A Song (Acoustic)").
        assertFalse(accepts("A Song", "Artist", 180, "A Song (Acoustic)", "Artist", 180))
    }

    // --- test_matching.py (score_candidate cases) ---

    @Test fun fuzzyMatchRejectsDifferentCreativeVersion() {
        assertFalse(accepts("Post Break-Up Sex", "The Vaccines", 174, "Post Break-Up Sex (Live in Brighton)", "The Vaccines", 174))
        assertFalse(accepts("Runaway", "AURORA", 243, "Runaway (Acoustic)", "AURORA", 243))
        assertTrue(accepts("Runaway - Piano Version", "AURORA", 243, "Runaway (Piano)", "AURORA", 243))
    }

    @Test fun scoreCandidateCases() {
        assertTrue(accepts("Elegia - 2015 Remaster", "New Order", 293, "Elegia", "New Order", 293))
        assertFalse(accepts("Runaway - Piano Version", "AURORA", 243, "Runaway", "AURORA", 309))
        assertTrue(accepts("Jeena Sikha De", listOf("Arijit Singh", "Ved Sharma", "Kunaal Vermaa"), 271, "Jeena Sikha De", "Arijit Singh", 271))
        assertTrue(accepts("Tri", "Popeye Bangladesh", 241, "Popeye (Bangladesh) - Tri (ত্রি) Official Music Video", "Popeye Bangladesh", 241))
        assertFalse(accepts("Oniket Prantor", "Artcell", 341, "Oniket Prantor", "Mehedi H Joy", 134))
    }

    @Test fun normalisationKeepsNonLatinScriptsAndTurkishDottedI() {
        // Turkish dotted I folds to i; ş folds to s (lib #70 accent folding, applied to both sides alike).
        assertEquals("iki seye pismanım", RecordingMatch.normalizeText("İki Şeye Pişmanım"))
        assertEquals("嘘", RecordingMatch.normalizeText("嘘"))
        assertEquals("जो तुम मेरे हो", RecordingMatch.normalizeText("जो तुम मेरे हो!"))
        assertEquals("GBSMU2629433", RecordingMatch.normalizeIsrc("gb-smu-26-29433"))
    }

    @Test fun punctuationOnlyArtistDrift() {
        assertTrue(accepts("Thunderstruck", "AC/DC", 292, "Thunderstruck", "ACDC", 292))
    }

    @Test fun wordBoundedMarkersDoNotFireInsideWords() {
        // The v1 FORBIDDEN_WORDS substring check fired "live" on "Alive" and "mix" on "Mixsingh".
        assertTrue(accepts("Alive", "Sia", 263, "Alive", "Sia", 263))
        assertTrue(RecordingMatch.versionMarkers("Stayin' Alive").isEmpty())
        assertTrue(RecordingMatch.versionMarkers("Delivery").isEmpty())
        assertEquals(setOf("live"), RecordingMatch.versionMarkers("Alive (Live)"))
    }

    // --- SoundBound fold-set fixtures (Docs/RESEARCH/song-identity-benchmark, real store results) ---

    @Test fun titleCleanupRules() {
        assertEquals("Paro", RecordingMatch.cleanTitle("Paro - @adityarikhari  UNPLG'd"))
        assertEquals("Arz Kiya Hai", RecordingMatch.cleanTitle("Arz Kiya Hai | Coke Studio Bharat"))
        assertEquals("Arz Kiya Hai", RecordingMatch.cleanTitle("Arz Kiya Hai  Coke Studio Bharat"))
        assertEquals("Pasoori", RecordingMatch.cleanTitle("Pasoori (Coke Studio Season 14)"))
        assertEquals("Jaane Na Tu", RecordingMatch.cleanTitle("Jaane Na Tu  (From \"UR Debut\")"))
        assertEquals("Woh Lamhe Woh Baatein", RecordingMatch.cleanTitle("Woh Lamhe Woh Baatein (From _Zeher_)"))
        assertEquals("Ve Haaniyaan", RecordingMatch.cleanTitle("Ve Haaniyaan (feat. Avvy Sra)"))
        assertEquals("Sahiba", RecordingMatch.cleanTitle("Sahiba (Official Video)"))
        assertEquals("Sahiba", RecordingMatch.cleanTitle("Sahiba [Official Music Video]"))
        assertEquals(setOf("live"), RecordingMatch.versionMarkers("Sahiba (Live Video)"))
    }

    @Test fun foldSetStoreHitsAreAccepted() {
        // YouTube source vs Apple/Spotify/Tidal hit; ±15 s applies because the source is YouTube.
        assertTrue(accepts("Midnight Call", listOf("Harkirat Sangha", "StarBoy X"), 163, "Midnight Call", "Harkirat Sangha & Starboy X", 164, yt = true))
        assertTrue(accepts("Barsaat", listOf("Banjaare", "Roni"), 186, "Barsaat", "Banjaare, Roni", 185, yt = true))
        assertTrue(accepts("Finding Her", listOf("Kushagra", "Bharath"), 213, "Finding Her", "Kushagra, Bharath, Saaheal", 207, yt = true))
        assertTrue(accepts("Jo Tum Mere Ho", "Anuv Jain", 258, "Jo Tum Mere Ho", "Anuv Jain", 252, yt = true))
        assertTrue(accepts("Sahiba", "Aditya Rikhari", 184, "Sahiba", "Aditya Rikhari", 190, yt = true))
        assertTrue(accepts("Paro - @adityarikhari  UNPLG'd", listOf("UNPLG'd", "Aditya Rikhari"), 71, "Paro", "Aditya Rikhari, UNPLG'd", 70, yt = true))
        assertTrue(accepts("Jaane Na Tu", listOf("Bhoomi", "Naaz", "Saaheal"), 191, "Jaane Na Tu (From \"UR Debut\")", "Bhoomi, Naaz, Saaheal", 190, yt = true))
        assertTrue(accepts("Arz Kiya Hai", "Anuv Jain", 306, "Arz Kiya Hai | Coke Studio Bharat", "Anuv Jain", 306, yt = true))
        // Live iTunes result (2026-11-08): 294 s against the 306 s upload.
        assertTrue(accepts("Arz Kiya Hai", "Anuv Jain", 306, "Arz Kiya Hai  Coke Studio Bharat", "Anuv Jain", 294, yt = true))
        assertTrue(accepts("Ve Haaniyaan (feat. Avvy Sra)", "Danny", 205, "Ve Haaniyaan", "Danny, Avvy Sra, Sagar", 205, yt = true))
        // Local file credits joined by \u0000 (SongEntity artist encoding).
        assertTrue(accepts("Kufar", "Diljit Dosanjh\u0000Mixsingh & Raj Ranjodh", 158, "Kufar", "Diljit Dosanjh, MixSingh, Raj Ranjodh", 158))
        assertTrue(accepts("Woh Lamhe Woh Baatein (From _Zeher_)", listOf("Atif Aslam", "Roop Kumar Rathod", "Mithoon", "Naresh Sharma"), 320,
            "Woh Lamhe Woh Baatein", "Roop Kumar Rathod, Mithoon, Naresh Sharma & Atif Aslam", 320))
        assertTrue(accepts("Alfaaz - Acoustic", listOf("Hamza Malik", "Zain Zohaib", "Atif Khan"), 236, "Alfaaz - Acoustic", "Hamza Malik, Zain Zohaib & Atif Khan", 236, yt = true))
    }

    @Test fun foldSetWrongRecordingsAreRejected() {
        // The benchmark's own pick for "Jhol" was the acoustic cut (closer duration: 278 s vs YouTube 279 s; studio is 266 s).
        assertFalse(accepts("Jhol", listOf("Maanu", "Annural Khalid"), 279, "Jhol (Acoustic)", "Maanu, Annural Khalid, Abdullah Siddiqui & Asfand", 278, yt = true))
        assertTrue(accepts("Jhol", listOf("Maanu", "Annural Khalid"), 279, "Jhol", "Maanu & Annural Khalid", 266, yt = true))
        // Gaana's "Alfaaz - Vocals" has no duration: the loose (subset) title tier needs one, so it is refused.
        assertFalse(accepts("Alfaaz", listOf("Hamza Malik", "Zain Zohaib"), 184, "Alfaaz - Vocals", "Hamza Malik,Zain Zohaib", 0))
        assertFalse(accepts("Alfaaz - Acoustic", listOf("Hamza Malik", "Zain Zohaib", "Atif Khan"), 236, "Alfaaz", "Hamza Malik, Zain Zohaib", 184, yt = true))
        // IFPI's "Finding Her" by kushagra & Showkidd (219 s) is a different recording.
        assertFalse(accepts("Finding Her", listOf("Kushagra", "Bharath"), 207, "Finding Her", "kushagra & Showkidd", 219))
        assertFalse(accepts("KALYANI (Remix)", listOf("ARJN", "KDS", "FIFTY4", "Shreya Ghoshal"), 288, "KALYANI", "ARJN, KDS, FIFTY4", 288, yt = true))
        // Store-to-store stays at ±3 s: the YouTube relaxation needs a YouTube side.
        assertFalse(accepts("Jo Tum Mere Ho", "Anuv Jain", 258, "Jo Tum Mere Ho", "Anuv Jain", 252))
        assertFalse(accepts("Sahiba", "Aditya Rikhari", 184, "Sahiba", "Various Artists", 184))
    }

    @Test fun topicChannelAndPlaceholderCredits() {
        assertTrue(accepts("One Love", "Shubh - Topic", 160, "One Love", "Shubh", 159))
        assertEquals(0.0, RecordingMatch.artistSimilarity(listOf("Various Artists"), listOf("Shubh")))
        assertEquals(listOf("Karan Aujla", "Ikky"), RecordingMatch.splitCredits(listOf("Karan Aujla x Ikky")))
    }

    @Test fun durationScoreIsExponential() {
        assertEquals(100f, calculateDurationMatch(200, 200))
        assertEquals(74.08f, calculateDurationMatch(203, 200), 0.01f)
        assertEquals(22.31f, calculateDurationMatch(185, 200), 0.01f)
    }

    // --- orderResults public API ---

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
        override fun ratio(a: String, b: String) = error("v2 must not call the bridge")
        override fun partialRatio(a: String, b: String) = error("v2 must not call the bridge")
    }

    @Test fun orderResultsIsBestFirstAndHonoursIsrcAndYoutubeWindow() {
        val wanted = P("Jhol", "Maanu & Annural Khalid", 279, trackUrl = "https://www.youtube.com/watch?v=-2RAq5o5pwc")
        val acoustic = P("Jhol (Acoustic)", "Maanu, Annural Khalid, Abdullah Siddiqui & Asfand", 278)
        val far = P("Jhol", "Maanu, Annural Khalid", 268)
        val studio = P("Jhol", "Maanu, Annural Khalid", 266)
        val near = P("Jhol", "Maanu & Annural Khalid", 279)
        val ordered = orderResults(wanted, listOf(acoustic, far, studio, near), NoFuzzy)
        assertEquals(listOf(near, far, studio), ordered.keys.toList())

        val byIsrc = P("Totally Different", "Nobody", 10, isrc = "QZDA62590546")
        assertEquals(listOf(byIsrc), orderResults(wanted.copy(isrc = "qz-da6-25-90546"), listOf(byIsrc), NoFuzzy).keys.toList())

        // Same pair without a YouTube side: 13 s off is outside the store window.
        assertTrue(orderResults(wanted.copy(trackUrl = null), listOf(studio), NoFuzzy).isEmpty())
    }

    // --- carried over from lib #70 (C-02) ---

    @Test fun accentsFoldSoSpellingDriftStillMatches() {
        assertTrue(accepts("Crazy in Love", "Beyoncé", 236, "Crazy In Love", "Beyonce", 236))
        assertEquals("deja vu", RecordingMatch.normalizeText("Déjà Vu"))
        assertEquals("केसरिया", RecordingMatch.normalizeText("केसरिया")) // Indic vowel signs untouched
    }

    @Test fun reactionAndReviewUploadsAreNotTheRecording() {
        assertFalse(accepts("Kesariya", "Arijit Singh", 268, "Kesariya Reaction", "Arijit Singh", 268, yt = true))
        assertFalse(accepts("Kesariya", "Arijit Singh", 268, "Kesariya (Review)", "Arijit Singh", 268, yt = true))
        assertTrue(accepts("Kesariya Reaction", "Arijit Singh", 268, "Kesariya Reaction", "Arijit Singh", 268))
    }

    @Test fun allSameLabelCatalogueStillYieldsClosestMatch() {
        val wanted = P("Pasoori", "Ali Sethi & Shae Gill", 224)
        val a = P("Pasoori (Live)", "Ali Sethi & Shae Gill", 224)
        val b = P("Pasoori (Live)", "Someone Else", 224)
        // Every candidate is labelled Live and the query is not: take the closest, best first.
        assertEquals(listOf(a), orderResults(wanted, listOf(a, b), NoFuzzy).keys.toList())
        // Mixed labels: the gate stays strict, no fallback.
        val remix = P("Pasoori (Remix)", "Ali Sethi & Shae Gill", 224)
        assertTrue(orderResults(wanted, listOf(a, remix), NoFuzzy).isEmpty())
        // The query asks for Live itself: normal path, no fallback needed.
        assertEquals(listOf(a), orderResults(wanted.copy(title = "Pasoori (Live)"), listOf(a, b), NoFuzzy).keys.toList())
    }
}
