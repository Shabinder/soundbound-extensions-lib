package `in`.shabinder.soundbound.models

import kotlinx.serialization.Serializable

@Serializable
data class QueryParams(
    val trackName: String,
    val trackArtists: List<String>,
    val trackDurationSec: Long,
    val genre: List<String> = emptyList(),
    val year: Int? = null,
    val albumName: String? = null,
    val albumArtists: List<String> = emptyList(),
)

fun SongModel.makeQueryParams(): QueryParams {
    return QueryParams(
        trackName = title,
        trackArtists = artists,
        trackDurationSec = durationSec,
        genre = genre,
        year = year,
        albumName = albumName,
        albumArtists = albumArtists ?: emptyList()
    )
}
