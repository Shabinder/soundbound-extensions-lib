package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days
@Parcelize
@Immutable
@Serializable
data class ChartListingModel(
    val uri: String,
    val name: String,
    val comment: String? = "",
    val thumbnail: String? = "",
    val list: List<ChartEntity>,
    val epochMs: Long,
    val subtitle: String? = null,
): Parcelable {
    constructor(
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
        get() = Instant.fromEpochMilliseconds(epochMs)
}

@Parcelize
@Immutable
@Serializable
data class ChartListingContainer(
    val name: String,
    val comment: String? = "",
    val thumbnail: String? = "",
    val chartListingModelFetcher: List<suspend () -> ChartListingModel> // on-req fetch
): Parcelable

@Parcelize
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
): BaseDownloadableModel() {
    @Parcelize
    @Immutable
    @Serializable
    enum class Type: Parcelable {
        SONG, PLAYLIST
    }
}