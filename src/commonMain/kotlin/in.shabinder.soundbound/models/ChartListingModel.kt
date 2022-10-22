package `in`.shabinder.soundbound.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

@Serializable
data class ChartListingModel(
    val uri: String,
    val name: String,
    val comment: String? = "",
    val thumbnail: String? = "",
    val list: List<ChartEntity>,
    val date: Instant
) {
    constructor(
        uri: String,
        name: String,
        comment: String? = "",
        thumbnail: String? = "",
        list: List<ChartEntity>
    ) : this(uri, name, comment, thumbnail, list, Clock.System.now())

    constructor(
        uri: String,
        name: String,
        comment: String? = "",
        thumbnail: String? = "",
        list: List<ChartEntity>,
        date: String
    ) : this(uri, name, comment, thumbnail, list, Instant.parse(date))

    val hasADayPassed: Boolean
        get() = Clock.System.now() - date > 1.days
}

@Serializable
data class ChartListingContainer(
    val name: String,
    val comment: String? = "",
    val thumbnail: String? = "",
    val chartListingModelFetcher: suspend () -> List<ChartListingModel>
)

@Serializable
data class ChartEntity(
    val songGuid: String,
    val title: String,
    val links: List<String>,
    val thumbnailURL: String?,
)