package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable


@Immutable
@Serializable
open class ChartListingModel(
    val uri: String,
    val name: String,
    val comment: String? = "",
    val thumbnail: String? = "",
    val list: List<ChartEntity>,
    val epochMs: Long,
    val subtitle: String? = null,
) {
    open fun copy(
        uri: String = this.uri,
        name: String = this.name,
        comment: String? = this.comment,
        thumbnail: String? = this.thumbnail,
        list: List<ChartEntity> = this.list,
        epochMs: Long = this.epochMs,
        subtitle: String? = this.subtitle,
    ) = ChartListingModel(uri, name, comment, thumbnail, list, epochMs, subtitle)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChartListingModel) return false

        if (uri != other.uri) return false
        if (name != other.name) return false
        if (comment != other.comment) return false
        if (thumbnail != other.thumbnail) return false
        if (list != other.list) return false
        if (epochMs != other.epochMs) return false
        if (subtitle != other.subtitle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (comment?.hashCode() ?: 0)
        result = 31 * result + (thumbnail?.hashCode() ?: 0)
        result = 31 * result + list.hashCode()
        result = 31 * result + epochMs.hashCode()
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ChartListingModel(uri='$uri', name='$name', comment=$comment, thumbnail=$thumbnail, list=$list, epochMs=$epochMs, subtitle=$subtitle)"
    }

    /*constructor(
        uri: String,
        name: String,
        comment: String? = "",
        thumbnail: String? = "",
        list: List<ChartEntity>,
        subtitle: String? = null,
    ) : this(uri, name, comment, thumbnail, list, Clock.System.now().toEpochMilliseconds(), subtitle)

    constructor(
        uri: String,
        name: String,
        comment: String? = "",
        thumbnail: String? = "",
        list: List<ChartEntity>,
        date: String,
        subtitle: String? = null,
    ) : this(uri, name, comment, thumbnail, list, Instant.parse(date).toEpochMilliseconds(), subtitle)

    constructor(
        uri: String,
        name: String,
        comment: String? = "",
        thumbnail: String? = "",
        list: List<ChartEntity>,
        date: Instant,
        subtitle: String? = null,
    ) : this(uri, name, comment, thumbnail, list, date.toEpochMilliseconds(), subtitle)

    val hasADayPassed: Boolean
        get() = Clock.System.now() - date > 1.days

    val date: Instant
        get() = Instant.fromEpochMilliseconds(epochMs)*/
}


@Immutable
@Serializable
data class ChartListingContainer(
    val name: String,
    val comment: String? = "",
    val thumbnail: String? = "",
    val chartListingModelFetcher: List<suspend () -> ChartListingModel> // on-req fetch
)


@Immutable
@Serializable
data class ChartEntity(
    val songGuid: String,
    val title: String,
    val subtitle: String,
    val links: List<String>,
    val thumbnailURL: String?,
    val sourceModel: SourceModel,
    val type: Type,
    override val downloaded: DownloadStatus = DownloadStatus.NotDownloaded,
) : BaseDownloadableModel() {

    @Immutable
    @Serializable
    enum class Type {
        SONG, PLAYLIST, ALBUM, ARTIST
    }
}