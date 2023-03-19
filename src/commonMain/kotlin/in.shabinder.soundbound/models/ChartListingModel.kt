package `in`.shabinder.soundbound.models

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days
@Parcelize
@Serializable
data class ChartListingModel(
    val uri: String,
    val name: String,
    val comment: String? = "",
    val thumbnail: String? = "",
    val list: List<ChartEntity>,
    val epochMs: Long,
): Parcelable {
    constructor(
        uri: String,
        name: String,
        comment: String? = "",
        thumbnail: String? = "",
        list: List<ChartEntity>
    ) : this(uri, name, comment, thumbnail, list, Clock.System.now().toEpochMilliseconds())

    constructor(
        uri: String,
        name: String,
        comment: String? = "",
        thumbnail: String? = "",
        list: List<ChartEntity>,
        date: String
    ) : this(uri, name, comment, thumbnail, list, Instant.parse(date).toEpochMilliseconds())

    constructor(
        uri: String,
        name: String,
        comment: String? = "",
        thumbnail: String? = "",
        list: List<ChartEntity>,
        date: Instant
    ) : this(uri, name, comment, thumbnail, list, date.toEpochMilliseconds())

    val hasADayPassed: Boolean
        get() = Clock.System.now() - date > 1.days

    val date: Instant
        get() = Instant.fromEpochMilliseconds(epochMs)
}

@Parcelize
@Serializable
data class ChartListingContainer(
    val name: String,
    val comment: String? = "",
    val thumbnail: String? = "",
    val chartListingModelFetcher: List<suspend () -> ChartListingModel> // on-req fetch
): Parcelable

@Parcelize
@Serializable
data class ChartEntity(
    val songGuid: String,
    val title: String,
    val links: List<String>,
    val thumbnailURL: String?,
    val sourceModel: SourceModel,
    val type: Type
): Parcelable {
    @Parcelize
    @Serializable
    enum class Type: Parcelable {
        SONG, PLAYLIST
    }
}