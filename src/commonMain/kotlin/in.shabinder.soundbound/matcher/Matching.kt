package `in`.shabinder.soundbound.matcher

import `in`.shabinder.soundbound.models.Artist
import `in`.shabinder.soundbound.zipline.FuzzySearch
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.max

interface MatchProps {
    val title: String
    val artists: List<Artist>
    val albumName: String?
    val durationSec: Long
    val isrc: String?

    /** Where this side came from, when known. A YouTube side gets the wider duration window (uploads add intros). */
    val trackUrl: String? get() = null
}

/**
 * Matching v2: the candidates that are the same recording as [matchFor], best first (the map iterates in score order,
 * so `keys.firstOrNull()` is the best match). An equal ISRC scores 100. Every other candidate must pass
 * [RecordingMatch.score]: version family (live/remix/acoustic/sped up/…, word-bounded, both directions), featured
 * guests, whole artist credits (tribute/karaoke guard), the duration window (±3 s, ±15 s when either side is
 * YouTube) and two-tier title similarity. Gates ported from songmirror (MIT), see [RecordingMatch].
 * [searcher] is kept for ABI compatibility; v2 scoring is self-contained so app and extensions agree.
 */
@Suppress("UNUSED_PARAMETER")
fun <S : MatchProps, T : MatchProps> orderResults(
    matchFor: T,
    allMatches: Collection<S>,
    searcher: FuzzySearch
): Map<S, Float> {
    val wantedIsrc = RecordingMatch.normalizeIsrc(matchFor.isrc)
    val wantedArtists = matchFor.artists.map(Artist::name)
    val scored = allMatches.mapNotNull { match ->
        if (wantedIsrc.isNotEmpty() && RecordingMatch.normalizeIsrc(match.isrc) == wantedIsrc) return@mapNotNull match to 100f
        val verdict = RecordingMatch.score(
            matchFor.title, wantedArtists, matchFor.durationSec,
            match.title, match.artists.map(Artist::name), match.durationSec,
            youtube = RecordingMatch.isYoutube(matchFor.trackUrl) || RecordingMatch.isYoutube(match.trackUrl),
        )
        if (verdict.accepted) match to verdict.score else null
    }
    if (scored.isNotEmpty() || allMatches.isEmpty()) return scored.sortedByDescending { it.second }.toMap(LinkedHashMap())
    // From lib #70 (C-02): a regional catalogue can label EVERY upload the same way ("Live", "Lofi").
    // When all candidates carry one identical version signature that the query lacks, judge them as
    // if that marker were absent, so the user still gets the closest one instead of nothing.
    val signatures = allMatches.map { RecordingMatch.versionSignature(it.title) }.toSet()
    if (signatures.size != 1 || signatures.single() == RecordingMatch.versionSignature(matchFor.title)) return emptyMap()
    return allMatches.mapNotNull { match ->
        val verdict = RecordingMatch.score(
            matchFor.title, wantedArtists, matchFor.durationSec,
            RecordingMatch.stripVersionWords(match.title),
            match.artists.map(Artist::name), match.durationSec,
            youtube = RecordingMatch.isYoutube(matchFor.trackUrl) || RecordingMatch.isYoutube(match.trackUrl),
        )
        if (verdict.accepted) match to verdict.score * 0.9f else null
    }.sortedByDescending { it.second }.toMap(LinkedHashMap())
}
fun getAlbumMatch(
    matchAlbumName: String?,
    matchForAlbumName: String?,
    searcher: FuzzySearch
): Float {
    if (matchAlbumName.isNullOrBlank() || matchForAlbumName.isNullOrBlank()) {
        return 0f
    }

    return searcher.ratio(matchAlbumName.sluggify(), matchForAlbumName.sluggify())
}

/** `100·e^(−0.1·|Δs|)`: 100 at 0 s, ~74 at 3 s, ~22 at 15 s (spotDL `matching.py`, MIT). */
fun calculateDurationMatch(matchDuration: Long, matchForDuration: Long): Float =
    (100.0 * exp(-0.1 * (matchDuration - matchForDuration).absoluteValue)).toFloat()

fun <T : MatchProps> calculateNameMatch(match: T, matchFor: T, searcher: FuzzySearch): Float {
    var nameMatch = getNameMatch(match.title, matchFor.title, searcher)

    // if too low, try with slugged artists
    if (nameMatch <= 75) {
        val matchTitleFilled =
            (match.title.sluggify().split("-") + match.sluggedArtists())
                .distinct().sorted()
                .joinToString("-")

        val matchForTitleFilled =
            (matchFor.title.sluggify().split("-") + matchFor.sluggedArtists())
                .distinct().sorted()
                .joinToString("-")

        val filledTitleMatch = searcher.ratio(matchTitleFilled, matchForTitleFilled)

        if (filledTitleMatch > nameMatch) {
            nameMatch = filledTitleMatch
        }
    }

    return nameMatch
}

fun getNameMatch(matchTitle: String, matchForTitle: String, searcher: FuzzySearch): Float {
    var match = searcher.ratio(matchForTitle, matchTitle)

    val matchTitleWords = matchTitle.sluggify().split("-").sorted()
    val matchForTitleWords = matchForTitle.sluggify().split("-").sorted()

    val sluggedSortedMatch =
        searcher.ratio(matchForTitleWords.joinToString("-"), matchTitleWords.joinToString("-"))

    if (sluggedSortedMatch > match) {
        match = sluggedSortedMatch
    }

    return match
}

fun getAllArtistsMatch(
    matchArtists: List<Artist>,
    matchForArtists: List<Artist>,
    searcher: FuzzySearch
): Float {
    val matchArtistsName = matchArtists.map(Artist::name)
    val matchForArtistsName = matchForArtists.map(Artist::name)

    var artistMatch = 0.0f

    if (matchForArtistsName.size == 1) return artistMatch

    var matchArtistsSlugged =
        (matchArtistsName.takeIf { it.size > 1 } ?: matchArtistsName.first().split(",").map(String::trim))
            .asSequence()
            .filter { it.isNotBlank() }.toSet().sorted()
            .map(String::sluggify).filter { it.isNotBlank() }
            .toList()

    val matchForArtistsSlugged =
        matchForArtistsName.asSequence()
            .filter { it.isNotBlank() }.toSet().sorted()
            .map(String::sluggify).filter { it.isNotBlank() }
            .toList()

    matchArtistsSlugged =
        matchArtistsSlugged.takeIf { it.size > 1 } ?: matchArtistsSlugged.first().split("-")
            .filter { it.isNotBlank() }

    var artistMatchNumber = 0f

    for ((matchArtist, matchForArtist) in matchArtistsSlugged.zip(matchForArtistsSlugged)) {
        val match = searcher.partialRatio(matchForArtist, matchArtist)
        artistMatchNumber += match
    }

    artistMatch = artistMatchNumber / matchForArtistsSlugged.size

    return artistMatch
}

fun getMainArtistMatch(
    matchArtists: List<Artist>,
    matchForArtists: List<Artist>,
    searcher: FuzzySearch
): Float {
    val matchArtistsName = matchArtists.map(Artist::name)
    val matchForArtistsName = matchForArtists.map(Artist::name)

    var mainArtistMatch: Float = 0f

    val matchArtistsSlugged =
        matchArtistsName.asSequence()
            .filter { it.isNotBlank() }.toSet()//.sorted()
            .map(String::sluggify).filter { it.isNotBlank() }
            .toList()

    val matchForArtistsSlugged =
        matchForArtistsName.asSequence()
            .filter { it.isNotBlank() }.toSet()//.sorted()
            .map(String::sluggify).filter { it.isNotBlank() }
            .toList()

    // Result match has no artists.
    if (matchArtistsSlugged.isEmpty() || matchForArtistsSlugged.isEmpty()) {
        return 0f
    }

    // check if main artist is in matchArtists

    // further break down the matchArtistsSlugged, since all artists probably are combined in one string
    if (matchForArtistsSlugged.size > 1 && matchArtistsSlugged.size == 1) {
        val matchArtistsFirstSlugged =
            matchArtistsSlugged.first().split("-").first { it.isNotBlank() }
        val mainArtistFirstSlugged = matchForArtistsSlugged.first()

        mainArtistMatch = searcher.ratio(matchArtistsFirstSlugged, mainArtistFirstSlugged)
        if (mainArtistMatch > 70) { // we have a pretty good match for main artist
            return mainArtistMatch
        }
    }

    val partialMainMatch = // some garbage is alongside possibly, so match partial
        searcher.partialRatio(matchForArtistsSlugged.first(), matchArtistsSlugged.first())

    mainArtistMatch = max(partialMainMatch, mainArtistMatch)

    // try to use other artists if first artist match is too low
    if (mainArtistMatch < 50f) {
        @Suppress("NAME_SHADOWING")
        val matchArtistsSlugged =
            matchArtistsSlugged.takeIf { it.size > 1 } ?: matchArtistsSlugged.first().split("-")
                .filter { it.isNotBlank() }


        for ((matchArtist, matchForArtist) in matchArtistsSlugged.sorted()
            .zip(matchForArtistsSlugged.sorted())) {
            val match = searcher.partialRatio(matchForArtist, matchArtist)
            if (match > mainArtistMatch) {
                mainArtistMatch = match
            }
        }
    }

    return mainArtistMatch
}

fun checkCommonWord(string1: String, string2: String): Boolean {
    val words1 = string1.sluggify().split("-").toSet()
    val words2 = string2.sluggify().split("-").toSet()

    for (word in words1) {
        if (word in words2) {
            // skip empty words
            if (word.isBlank()) continue

            return true
        }
    }

    return false
}

private fun <T : MatchProps> T.sluggedArtists(): List<String> {
    val artistsNames = artists.map(Artist::name)

    return artistsNames.map(String::sluggify).takeIf { it.size > 1 }
        ?: artistsNames.firstOrNull()?.split("-")?.map(String::sluggify)
        ?: artistsNames
}