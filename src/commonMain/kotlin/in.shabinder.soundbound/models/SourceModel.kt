package `in`.shabinder.soundbound.models

@kotlinx.serialization.Serializable
open class SourceModel(
    open val sourceName: String,
    open val sourceURL: String,
    open val isActive: Boolean = true
) {
    @kotlin.jvm.JvmOverloads
    open fun copy(
        sourceName: String = this.sourceName,
        sourceURL: String = this.sourceURL,
        isActive: Boolean = this.isActive
    ): SourceModel {
        return SourceModel(
            sourceName = sourceName,
            sourceURL = sourceURL,
            isActive = isActive
        )
    }
}
