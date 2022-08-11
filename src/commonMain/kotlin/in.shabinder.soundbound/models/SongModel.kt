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

@Serializable
open class SongModel(
    open val id: Long,
    open val title: String,
    open val playListID: Long,
    open val durationSec: Long,
    open val year: Int,
    open val dateModified: Long,
    open val artists: List<String>,
    open val genre: List<String>,
    open val albumName: String?,
    open val albumArtists: List<String>?,
    open val trackNumber: Long?,
    open val comment: String?,
    open val trackURL: String?,
    open val albumArtURL: String?,
    open val source: SourceModel,
    open val downloadLink: String?,
    @Contextual open val downloaded: DownloadStatus = DownloadStatus.NotDownloaded,
    open val audioQuality: AudioQuality = AudioQuality.KBPS192,
    open val audioFormat: AudioFormat = AudioFormat.MP4,
    open val videoID: String?,
)