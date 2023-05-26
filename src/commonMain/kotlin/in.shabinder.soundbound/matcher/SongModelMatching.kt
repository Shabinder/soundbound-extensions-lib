package `in`.shabinder.soundbound.matcher

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.SongModel

@Immutable
data class WrapperWithMatcherProps<T>(val model: T, val matchProps: MatchProps) :
    MatchProps by matchProps

fun orderResults(
    matchFor: SongModel,
    allMatches: List<SongModel>,
): Map<SongModel, Float> {
    val songModelWithMatchProps = allMatches.map { WrapperWithMatcherProps(it, it.asMatchProps()) }
    return orderResults(
        WrapperWithMatcherProps(matchFor, matchFor.asMatchProps()),
        songModelWithMatchProps
    ).mapKeys { it.key.model }
}

fun SongModel.asMatchProps(): MatchProps {
    val song = this
    return object : MatchProps {
        override val title: String = song.title
        override val artists: List<String> = song.artists
        override val albumName: String? = song.albumName
        override val durationSec: Long = song.durationSec
        override val isrc: String? = song.isrc
    }
}
