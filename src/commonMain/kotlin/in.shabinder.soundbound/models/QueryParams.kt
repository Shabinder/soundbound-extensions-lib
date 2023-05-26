package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import `in`.shabinder.soundbound.utils.cleaned
import kotlinx.serialization.Serializable

@Parcelize
@Immutable
@Serializable
open class QueryParams(
    open val trackName: String,
    open val trackArtists: List<String> = emptyList(),
    open val trackDurationSec: Long,
    open val genre: List<String> = emptyList(),
    open val year: Int? = null,
    open val albumName: String? = null,
    open val albumArtists: List<String> = emptyList(),
    open val trackLink: String? = null,
    open val interestedEntityType: SearchItem.Type = SearchItem.Type.All,
    open val isrc: String? = null,
) : Parcelable {

    @kotlin.jvm.JvmOverloads
    open fun copy(
        trackName: String = this.trackName,
        trackArtists: List<String> = this.trackArtists,
        trackDurationSec: Long = this.trackDurationSec,
        genre: List<String> = this.genre,
        year: Int? = this.year,
        albumName: String? = this.albumName,
        albumArtists: List<String> = this.albumArtists,
        trackLink: String? = this.trackLink,
        interestedEntityType: SearchItem.Type = this.interestedEntityType,
        isrc: String? = this.isrc,
    ): QueryParams {
        return QueryParams(
            trackName = trackName,
            trackArtists = trackArtists,
            trackDurationSec = trackDurationSec,
            genre = genre,
            year = year,
            albumName = albumName,
            albumArtists = albumArtists,
            trackLink = trackLink,
            interestedEntityType = interestedEntityType,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QueryParams) return false
        if (trackName != other.trackName) return false
        if (trackDurationSec != other.trackDurationSec) return false
        if (year != other.year) return false
        if (albumName != other.albumName) return false
        if (genre.cleaned() != other.genre.cleaned()) return false
        if (trackArtists.cleaned() != other.trackArtists.cleaned()) return false
        if (albumArtists.cleaned() != other.albumArtists.cleaned()) return false
        if (trackLink != other.trackLink) return false
        if (interestedEntityType != other.interestedEntityType) return false
        if (isrc != other.isrc) return false
        return true
    }

    override fun hashCode(): Int {
        var result = trackName.hashCode()
        result = 31 * result + trackArtists.hashCode()
        result = 31 * result + trackDurationSec.hashCode()
        result = 31 * result + genre.hashCode()
        result = 31 * result + (year ?: 0)
        result = 31 * result + (albumName?.hashCode() ?: 0)
        result = 31 * result + albumArtists.hashCode()
        result = 31 * result + (trackLink?.hashCode() ?: 0)
        result = 31 * result + interestedEntityType.hashCode()
        result = 31 * result + (isrc?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "QueryParams(trackName=$trackName, trackArtists=$trackArtists, trackDurationSec=$trackDurationSec, genre=$genre, year=$year, albumName=$albumName, albumArtists=$albumArtists, trackLink=$trackLink, interestedEntityType=$interestedEntityType, ISRC=$isrc)"
    }
}
