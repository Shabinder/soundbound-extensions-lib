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

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

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
    open val trackURL: String?,
    open val albumArtURL: String?,
    open val downloadLink: String?,
    open val audioQuality: AudioQuality = AudioQuality.KBPS192,
    open val audioFormat: AudioFormat = AudioFormat.MP4,
    @Contextual open val downloaded: DownloadStatus = DownloadStatus.NotDownloaded,
    open val isFavourite: Boolean = false,
) {

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
        trackURL: String? = this.trackURL,
        albumArtURL: String? = this.albumArtURL,
        downloadLink: String? = this.downloadLink,
        audioQuality: AudioQuality = this.audioQuality,
        audioFormat: AudioFormat = this.audioFormat,
        downloaded: DownloadStatus = this.downloaded,
        isFavourite: Boolean = this.isFavourite
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
            isFavourite = isFavourite
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
            albumArtists = albumArtists
        )
    }
}