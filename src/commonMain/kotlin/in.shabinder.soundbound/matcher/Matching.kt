package `in`.shabinder.soundbound.matcher

import `in`.shabinder.soundbound.models.Artist
import `in`.shabinder.soundbound.zipline.FuzzySearch
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

interface MatchProps {
    val title: String
    val artists: List<Artist>
    val albumName: String?
    val durationSec: Long
    val isrc: String?
}

fun <S : MatchProps, T : MatchProps> orderResults(
    matchFor: T,
    allMatches: Collection<S>,
    searcher: FuzzySearch
): Map<S, Float> {
    val resultsWithMatchValue = mutableMapOf<S, Float>()

    for (match in allMatches) {
        // skip results that have no common words in their name
        if (!checkCommonWord(match.title.lowercase(), matchFor.title.lowercase())) {
            continue
        }

        var artistMatch = getMainArtistMatch(match.artists, matchFor.artists, searcher)
        val otherArtistsMatch = getAllArtistsMatch(match.artists, matchFor.artists, searcher)

        artistMatch += otherArtistsMatch
        artistMatch /= if (matchFor.artists.size > 1) 2 else 1

        // TODO artist match fixes

        val nameMatch = calculateNameMatch(match, matchFor, searcher)

        // degrade this match if includes one of the forbidden words
        val totalForbiddenOccurrences = FORBIDDEN_WORDS.mapNotNull {
            //match contains while matchFor doesn't
            if (match.title.contains(it, ignoreCase = true)
                && !matchFor.title.contains(it, ignoreCase = true)
            ) it
            else null
        }

        val finalNameMatch = nameMatch - (totalForbiddenOccurrences.size * 10)

        if (finalNameMatch < 50) {
            // ignoring matches with too low name match ratio
            continue
        }

        if (artistMatch < 50) {
            // ignoring matches with too low artist match ratio
            continue
        }


        // Calculate total match
        var avgMatch = (artistMatch + nameMatch) / 2

        // add album match if certain
        val albumMatch = getAlbumMatch(match.albumName, matchFor.albumName, searcher)
        if (!match.isrc.isNullOrBlank() && albumMatch != 0f && albumMatch <= 80f) {
            avgMatch += albumMatch
            avgMatch /= 2
        }

        val durationMatch = calculateDurationMatch(match.durationSec, matchFor.durationSec)

        if (durationMatch < 50 && avgMatch < 75) {
            // ignoring matches with too low duration match ratio
            continue
        }

        if (!match.isrc.isNullOrBlank() && avgMatch <= 75 && durationMatch <= 75) {
            // add duration into avg if match is certain and confident
            avgMatch += durationMatch
            avgMatch /= 2
        }

        avgMatch = min(avgMatch, 100f)

        resultsWithMatchValue[match] = avgMatch
    }

    return resultsWithMatchValue
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

fun calculateDurationMatch(matchDuration: Long, matchForDuration: Long): Float {
    return 100f - (matchDuration - matchForDuration).absoluteValue
}

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

@Suppress("PrivatePropertyName")
private val FORBIDDEN_WORDS = listOf(
    "bassboosted",
    "remix",
    "remastered",
    "remaster",
    "reverb",
    "bassboost",
    "live",
    "acoustic",
    "8daudio",
)

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