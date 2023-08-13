/*
 * Copyright (c)  2021  Shabinder Singh
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.parcelize.Parcelize
import `in`.shabinder.soundbound.utils.cleaned
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Parcelize
@Immutable
@Serializable
open class SongModel(
    open val id: Long,
    open val title: String,
    open val durationSec: Long,
    open val year: Int,
    open val artists: List<String>,
    open val genre: List<String>,
    open val source: SourceModel,
    open val videoID: String?,
    open val albumName: String?,
    open val albumArtists: List<String>,
    open val trackNumber: Long?,
    open val comment: String?,
    open val trackURL: String,
    open val albumArtURL: String?,
    open val downloadLink: Request?,
    open val audioQuality: AudioQuality = AudioQuality.KBPS192,
    open val audioFormat: AudioFormat = AudioFormat.MP4,
    override val downloaded: DownloadStatus = DownloadStatus.NotDownloaded,
    open val isFavourite: Boolean = false,
    open val isrc: String? = null,
    open val extraProps: Map<String, String> = emptyMap()
) : BaseDownloadableModel() {

    @JvmOverloads
    open fun copy(
        id: Long = this.id,
        title: String = this.title,
        durationSec: Long = this.durationSec,
        year: Int = this.year,
        artists: List<String> = this.artists,
        genre: List<String> = this.genre,
        source: SourceModel = this.source,
        videoID: String? = this.videoID,
        albumName: String? = this.albumName,
        albumArtists: List<String> = this.albumArtists,
        trackNumber: Long? = this.trackNumber,
        comment: String? = this.comment,
        trackURL: String = this.trackURL,
        albumArtURL: String? = this.albumArtURL,
        downloadLink: Request? = this.downloadLink,
        audioQuality: AudioQuality = this.audioQuality,
        audioFormat: AudioFormat = this.audioFormat,
        downloaded: DownloadStatus = this.downloaded,
        isFavourite: Boolean = this.isFavourite,
        isrc: String? = this.isrc,
        extraProps: Map<String, String> = this.extraProps
    ): SongModel {
        return SongModel(
            id = id,
            title = title,
            durationSec = durationSec,
            year = year,
            artists = artists,
            genre = genre,
            source = source,
            videoID = videoID,
            albumName = albumName,
            albumArtists = albumArtists,
            trackNumber = trackNumber,
            comment = comment,
            trackURL = trackURL,
            albumArtURL = albumArtURL,
            downloadLink = downloadLink,
            audioQuality = audioQuality,
            audioFormat = audioFormat,
            downloaded = downloaded,
            isFavourite = isFavourite,
            isrc = isrc,
            extraProps = extraProps
        )
    }


    open fun makeQueryParams(): QueryParams {
        return QueryParams(
            trackName = title,
            trackArtists = artists,
            trackDurationSec = durationSec,
            genre = genre,
            year = year,
            albumName = albumName,
            albumArtists = albumArtists,
            trackLink = trackURL,
            isrc = isrc,
        )
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SongModel) return false
        if (id != other.id) return false
        if (title != other.title) return false
        if (durationSec != other.durationSec) return false
        if (year != other.year) return false
        if (source != other.source) return false
        if (videoID != other.videoID) return false
        if (albumName != other.albumName) return false
        if (trackNumber != other.trackNumber) return false
        if (comment != other.comment) return false
        if (trackURL != other.trackURL) return false
        if (albumArtURL != other.albumArtURL) return false
        if (downloadLink != other.downloadLink) return false
        if (audioQuality != other.audioQuality) return false
        if (audioFormat != other.audioFormat) return false
        if (downloaded != other.downloaded) return false
        if (isFavourite != other.isFavourite) return false
        if (artists.cleaned() != other.artists.cleaned()) return false
        if (albumArtists.cleaned() != other.albumArtists.cleaned()) return false
        if (genre.cleaned() != other.genre.cleaned()) return false
        if (isrc != other.isrc) return false
        if (extraProps != other.extraProps) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + durationSec.hashCode()
        result = 31 * result + year
        result = 31 * result + artists.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + (videoID?.hashCode() ?: 0)
        result = 31 * result + (albumName?.hashCode() ?: 0)
        result = 31 * result + albumArtists.hashCode()
        result = 31 * result + (trackNumber?.hashCode() ?: 0)
        result = 31 * result + (comment?.hashCode() ?: 0)
        result = 31 * result + trackURL.hashCode()
        result = 31 * result + (albumArtURL?.hashCode() ?: 0)
        result = 31 * result + (downloadLink?.hashCode() ?: 0)
        result = 31 * result + audioQuality.hashCode()
        result = 31 * result + audioFormat.hashCode()
        result = 31 * result + downloaded.hashCode()
        result = 31 * result + isFavourite.hashCode()
        result = 31 * result + genre.hashCode()
        result = 31 * result + (isrc?.hashCode() ?: 0)
        result = 31 * result + extraProps.hashCode()
        return result
    }

    override fun toString(): String {
        return "SongModel(id=$id, title=$title, durationSec=$durationSec, year=$year, artists=$artists, genre=$genre, source=$source, videoID=$videoID, albumName=$albumName, albumArtists=$albumArtists, trackNumber=$trackNumber, comment=$comment, trackURL=$trackURL, albumArtURL=$albumArtURL, downloadLink=$downloadLink, audioQuality=$audioQuality, audioFormat=$audioFormat, downloaded=$downloaded, isFavourite=$isFavourite, isrc=$isrc, extraProps=$extraProps)"
    }
}