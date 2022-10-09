package `in`.shabinder.soundbound.models

import kotlinx.serialization.Serializable

@Serializable
open class QueryParams(
    open val trackName: String,
    open val trackArtists: List<String>,
    open val trackDurationSec: Long,
    open val genre: List<String> = emptyList(),
    open val year: Int? = null,
    open val albumName: String? = null,
    open val albumArtists: List<String> = emptyList(),
) {
    @kotlin.jvm.JvmOverloads
    open fun copy(
        trackName: String = this.trackName,
        trackArtists: List<String> = this.trackArtists,
        trackDurationSec: Long = this.trackDurationSec,
        genre: List<String> = this.genre,
        year: Int? = this.year,
        albumName: String? = this.albumName,
        albumArtists: List<String> = this.albumArtists,
    ): QueryParams {
        return QueryParams(
            trackName = trackName,
            trackArtists = trackArtists,
            trackDurationSec = trackDurationSec,
            genre = genre,
            year = year,
            albumName = albumName,
            albumArtists = albumArtists,
        )
    }
}
