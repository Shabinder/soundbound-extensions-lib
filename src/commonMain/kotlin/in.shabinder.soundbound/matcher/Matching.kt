package `in`.shabinder.soundbound.matcher

import `in`.shabinder.soundbound.models.Artist
import `in`.shabinder.soundbound.zipline.FuzzySearch
import kotlin.math.max

interface MatchProps {
    val title: String
    val artists: List<Artist>
    val albumName: String?
    val durationSec: Long
    val isrc: String?

    /** Where this side came from, when known. */
    val trackUrl: String? get() = null

    /** This side's length may include intros/outros, so it gets [RecordingMatch.LOOSE_TOLERANCE_SEC]. */
    val looseDuration: Boolean get() = RecordingMatch.hasLooseDuration(trackUrl)
}

/**
 * Matching v2: the candidates that are the same recording as [matchFor], best first (the map iterates in score order,
 * so `keys.firstOrNull()` is the best match). Scores only; [rankMatches] also gives each verdict's reason/confidence.
 * [searcher] is kept for ABI compatibility; v2 scoring is self-contained so app and extensions agree.
 */
@Suppress("UNUSED_PARAMETER")
fun <S : MatchProps, T : MatchProps> orderResults(
    matchFor: T,
    allMatches: Collection<S>,
    searcher: FuzzySearch
): Map<S, Float> = rankMatches(matchFor, allMatches).associateTo(LinkedHashMap()) { it.first to it.second.score }

/**
 * The accepted candidates with their [MatchVerdict], best first. An equal ISRC scores 100 ([MatchReason.ISRC]) unless
 * known metadata contradicts it ([RecordingMatch.idConflict]: a search that tags the wrong upload with the wanted code
 * must not win). Every other candidate must pass [RecordingMatch.score]: version family (live/remix/acoustic/sped
 * up/…, word-bounded, both directions), featured guests, whole artist credits (tribute/karaoke guard), the duration
 * window (±3 s, ±15 s when either side has loose durations) and two-tier title similarity. Gates ported from songmirror and
 * SoulSync (MIT), see [RecordingMatch].
 */
fun <S : MatchProps, T : MatchProps> rankMatches(matchFor: T, allMatches: Collection<S>): List<Pair<S, MatchVerdict>> {
    val wantedIsrc = RecordingMatch.normalizeIsrc(matchFor.isrc)
    val wantedArtists = matchFor.artists.map(Artist::name)
    fun yt(match: S) = matchFor.looseDuration || match.looseDuration
    fun verdict(match: S, title: String = match.title, trustIsrc: Boolean = true): MatchVerdict {
        val candArtists = match.artists.map(Artist::name)
        if (trustIsrc && wantedIsrc.isNotEmpty() && RecordingMatch.normalizeIsrc(match.isrc) == wantedIsrc &&
            RecordingMatch.idConflict(matchFor.title, wantedArtists, matchFor.durationSec, title, candArtists, match.durationSec, yt(match)) == null
        ) return MatchVerdict(100f, true, MatchReason.ISRC)
        return RecordingMatch.score(matchFor.title, wantedArtists, matchFor.durationSec, title, candArtists, match.durationSec, looseDuration = yt(match))
    }
    val scored = allMatches.map { it to verdict(it) }.filter { it.second.accepted }
    // A perfect text match also scores 100; on ties the code-proven candidate goes first.
    if (scored.isNotEmpty() || allMatches.isEmpty()) {
        return scored.sortedWith(compareByDescending<Pair<S, MatchVerdict>> { it.second.score }.thenByDescending { it.second.reason == MatchReason.ISRC })
    }
    // From lib #70 (C-02): a regional catalogue can label EVERY upload the same way ("Live", "Lofi").
    // When all candidates carry one identical version signature that the query lacks, judge them as
    // if that marker were absent, so the user still gets the closest one instead of nothing.
    val signatures = allMatches.map { RecordingMatch.versionSignature(it.title) }.toSet()
    if (signatures.size != 1 || signatures.single() == RecordingMatch.versionSignature(matchFor.title)) return emptyList()
    return allMatches.mapNotNull { match ->
        // Text only: an ISRC on a relabelled title proves nothing about which cut it is.
        val v = verdict(match, RecordingMatch.stripVersionWords(match.title), trustIsrc = false)
        if (v.accepted) match to MatchVerdict(v.score * 0.9f, true, MatchReason.SAME_LABEL_FALLBACK) else null
    }.sortedByDescending { it.second.score }
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

/** [RecordingMatch.durationBand] on 0..100: 100 within 1 s, 90 within 2.5 s, 70 within 5 s, … 0 past 15 s. */
fun calculateDurationMatch(matchDuration: Long, matchForDuration: Long): Float =
    (100.0 * RecordingMatch.durationBand(matchDuration - matchForDuration)).toFloat()

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