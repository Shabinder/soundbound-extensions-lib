package `in`.shabinder.soundbound.models

import kotlinx.serialization.Serializable

@Serializable
data class QueryParams(
    val trackName: String,
    val trackArtists: List<String>,
    val trackDurationSec: Int,
    val genre: List<String> = emptyList(),
    val year: Int? = null,
    val albumName: String? = null,
)

fun TrackDetails.makeQueryParams(): QueryParams {
    return QueryParams(
        trackName = title,
        trackArtists = artists,
        trackDurationSec = durationSec,
        genre = genre,
        year = year,
        albumName = albumName,
    )
}
