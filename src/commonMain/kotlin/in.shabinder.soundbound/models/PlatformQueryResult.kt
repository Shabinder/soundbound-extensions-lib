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

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
@Parcelize
open class PlatformQueryResult(
    open val title: String,
    open val coverUrl: String,
    open val queryLink: String,
    open val trackList: List<SongModel>,
    open val source: SourceModel,
    open val description: String = "",
    open val creators: List<String> = emptyList(),
): Parcelable {
    @JvmOverloads
    fun copy(
        title: String = this.title,
        coverUrl: String = this.coverUrl,
        queryLink: String = this.queryLink,
        trackList: List<SongModel> = this.trackList,
        source: SourceModel = this.source,
        description: String = this.description,
        creators: List<String> = this.creators,
    ): PlatformQueryResult {
        return PlatformQueryResult(
            title = title,
            coverUrl = coverUrl,
            queryLink = queryLink,
            trackList = trackList,
            source = source,
            description = description,
            creators = creators,
        )
    }

    override fun toString(): String {
        return "PlatformQueryResult(title='$title', coverUrl='$coverUrl', queryLink='$queryLink', trackList=$trackList, source=$source, description='$description', creators=$creators)"
    }
}
