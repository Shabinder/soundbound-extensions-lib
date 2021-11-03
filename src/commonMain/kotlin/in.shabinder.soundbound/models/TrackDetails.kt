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
data class TrackDetails(
    var title: String,
    var artists: List<String>,
    var durationSec: Int,
    var albumName: String? = null,
    var albumArtists: List<String> = emptyList(),
    var genre: List<String> = emptyList(),
    var trackNumber: Int? = null,
    var year: Int? = null,
    var comment: String? = null,
    var lyrics: String? = null,
    var trackUrl: String? = null,
    var albumArtPath: String,
    var albumArtURL: String?,
    var source: String,
    val progress: Int = 2,
    var downloadLink: String? = null,
    @Contextual val downloaded: DownloadStatus = DownloadStatus.NotDownloaded,
    var audioQuality: AudioQuality = AudioQuality.KBPS192,
    var audioFormat: AudioFormat = AudioFormat.MP4,
    var outputFilePath: String,
    var videoID: String? = null, // will be used for purposes like Downloadable Link || VideoID etc. based on Provider
) {

    @Serializable
    data class DownloadQueryResult(
        val downloadURL: String,
        val audioFormat: AudioFormat,
        val audioQuality: AudioQuality
    ) {
        override fun toString(): String {
            return """
                DownloadLinkResult(
                    downloadURL = $downloadURL,
                    audioFormat = ${audioFormat.name},
                    audioQuality = ${audioQuality.name}
                )
            """.trimIndent()
        }
    }
}