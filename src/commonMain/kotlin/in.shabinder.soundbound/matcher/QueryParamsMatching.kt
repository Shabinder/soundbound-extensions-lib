package `in`.shabinder.soundbound.matcher

import `in`.shabinder.soundbound.models.Artist
import `in`.shabinder.soundbound.models.QueryParams
import `in`.shabinder.soundbound.zipline.FuzzySearch

fun <T : MatchProps> orderResults(
    matchFor: QueryParams,
    allMatches: List<T>,
    searcher: FuzzySearch
): Map<T, Float> {
    val songModelWithMatchProps = allMatches.map { WrapperWithMatcherProps(it, it) }
    return orderResults(
        WrapperWithMatcherProps(matchFor, matchFor.asMatchProps()),
        songModelWithMatchProps,
        searcher
    ).mapKeys { it.key.model }
}

fun QueryParams.asMatchProps(): MatchProps {
    val queryParams = this
    return object : MatchProps {
        override val title: String = queryParams.trackName
        override val artists: List<Artist> = queryParams.trackArtists
        override val albumName: String? = queryParams.albumName
        override val durationSec: Long = queryParams.trackDurationSec
        override val isrc: String? = queryParams.isrc
    }
}
