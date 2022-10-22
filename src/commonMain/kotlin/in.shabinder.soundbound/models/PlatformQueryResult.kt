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

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
open class PlatformQueryResult(
    open val title: String,
    open val coverUrl: String,
    open val queryLink: String,
    open val trackList: List<SongModel>,
    open val source: SourceModel
) {
    @JvmOverloads
    fun copy(
        title: String = this.title,
        coverUrl: String = this.coverUrl,
        queryLink: String = this.queryLink,
        trackList: List<SongModel> = this.trackList,
        source: SourceModel = this.source
    ): PlatformQueryResult {
        return PlatformQueryResult(
            title = title,
            coverUrl = coverUrl,
            queryLink = queryLink,
            trackList = trackList,
            source = source
        )
    }

    override fun toString(): String {
        return "PlatformQueryResult(title=$title, coverUrl=$coverUrl, queryLink=$queryLink, trackList=$trackList, source=$source)"
    }
}
