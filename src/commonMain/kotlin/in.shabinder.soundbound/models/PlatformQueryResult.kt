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


import `in`.shabinder.soundbound.utils.cleaned
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads


@Immutable
@Serializable
open class PlatformQueryResult(
    open val title: String,
    open val coverUrl: String,
    open val queryLink: String,
    open val trackList: List<SongModel>,
    open val source: SourceModel,
    open val description: String = "",
    open val creators: List<Artist> = emptyList(),
) {
    @JvmOverloads
    open fun copy(
        title: String = this.title,
        coverUrl: String = this.coverUrl,
        queryLink: String = this.queryLink,
        trackList: List<SongModel> = this.trackList,
        source: SourceModel = this.source,
        description: String = this.description,
        creators: List<Artist> = this.creators,
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformQueryResult) return false
        if (title != other.title) return false
        if (coverUrl != other.coverUrl) return false
        if (queryLink != other.queryLink) return false
        if (trackList != other.trackList) return false
        if (source != other.source) return false
        if (description != other.description) return false
        if (creators.cleaned() != other.creators.cleaned()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + coverUrl.hashCode()
        result = 31 * result + queryLink.hashCode()
        result = 31 * result + trackList.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + creators.cleaned().hashCode()
        return result
    }

    override fun toString(): String {
        return "PlatformQueryResult(title='$title', coverUrl='$coverUrl', queryLink='$queryLink', trackList=$trackList, source=$source, description='$description', creators=$creators)"
    }
}
