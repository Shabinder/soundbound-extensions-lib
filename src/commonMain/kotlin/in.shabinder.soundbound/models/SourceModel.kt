package `in`.shabinder.soundbound.models

@kotlinx.serialization.Serializable
data class SourceModel(
    val sourceName: String,
    val sourceURL: String,
)
