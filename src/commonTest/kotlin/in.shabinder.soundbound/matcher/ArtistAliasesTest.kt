package `in`.shabinder.soundbound.matcher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArtistAliasesTest {
    private fun same(a: String, b: String) = RecordingMatch.artistSimilarity(listOf(a), listOf(b)) >= RecordingMatch.ARTIST_THRESHOLD

    @Test fun recordedNamesOfOneArtistMatchAcrossScripts() {
        for ((a, b) in listOf(
            "王菲" to "Faye Wong", "王靖雯" to "王菲", "동방신기" to "TVXQ!", "東方神起" to "동방신기",
            "宇多田ヒカル" to "Hikaru Utada", "BTS" to "방탄소년단", "中澤ゆうこ" to "Yuuko Nakazawa",
            "奥田民生" to "Tamio Okuda", "東京スカパラダイスオーケストラ" to "Tokyo Ska Paradise Orchestra",
        )) assertTrue(same(a, b) && same(b, a), "$a / $b")
    }

    @Test fun aliasesDoNotMergeDifferentArtists() {
        // Same pinyin reading (wang fei), different people.
        assertFalse(same("王飞", "Faye Wong"))
        assertFalse(same("王菲", "TVXQ!"))
        assertFalse(same("BTS", "TVXQ!"))
        assertFalse(RecordingMatch.artistSimilarity(listOf("王菲"), listOf("Faye Wong"), aliases = false) >= RecordingMatch.ARTIST_THRESHOLD)
    }

    @Test fun everyCreditMustStillAppear() {
        // An alias hit on one credit does not excuse a missing second credit.
        val t = RecordingMatch.ARTIST_THRESHOLD
        assertTrue(RecordingMatch.artistSimilarity(listOf("동방신기", "BoA"), listOf("TVXQ!", "BoA")) >= t)
        assertFalse(RecordingMatch.artistSimilarity(listOf("동방신기", "BoA"), listOf("TVXQ!", "IU")) >= t)
    }

    @Test fun tributeGuardStillWins() {
        assertEquals(0.0, RecordingMatch.artistSimilarity(listOf("王菲"), listOf("Faye Wong Tribute")))
    }

    @Test fun aliasCarriesTheArtistGateOnly() {
        // Same recording, store metadata in two scripts: accepted.
        assertTrue(RecordingMatch.score("悶人咖啡", listOf("王菲"), 263, "悶人咖啡", listOf("Faye Wong"), 262).accepted)
        // Another song by the same artist at the same length: the title still rejects.
        assertFalse(RecordingMatch.score("悶人咖啡", listOf("王菲"), 263, "My Loneliness", listOf("Faye Wong"), 263).accepted)
        // Two karaoke tracks whose only shared words are オリジナル・カラオケ (version words are stripped from the reading).
        assertFalse(RecordingMatch.score("純情行進曲 (オリジナル・カラオケ)", listOf("中澤ゆうこ"), 200, "平成ラプソディー (オリジナル・カラオケ)", listOf("Yuuko Nakazawa"), 200).accepted)
    }

    @Test fun registeredNamesMatch() {
        assertFalse(same("가상의가수", "Imaginary Singer"))
        ArtistAliases.register(listOf("가상의가수", "Imaginary Singer"))
        assertTrue(same("가상의가수", "Imaginary Singer"))
        // A registered name cannot pull a table artist's key into another group.
        ArtistAliases.register(listOf("王菲", "Someone Else"))
        assertFalse(same("王菲", "Someone Else"))
    }

    @Test fun codecRoundTrips() {
        val text = "beatlesthe|битлз|ビートルズ|披头士\nfayewong|王菲|王靖雯\nbts|방탄소년단|防弾少年団\nx|\u4e00\uffff\u0370"
        val encoded = ArtistAliasCodec.encode(text)
        assertTrue(encoded.all { it.code in 32..126 && it != '"' && it != '\\' && it != '$' })
        assertEquals(text, ArtistAliasCodec.decode(encoded))
    }
}
