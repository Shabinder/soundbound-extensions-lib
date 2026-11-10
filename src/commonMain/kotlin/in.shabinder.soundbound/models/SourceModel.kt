package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable


@Immutable
@Serializable
open class SourceModel(
    open val sourceName: String,
    open val sourceURL: String,
    open val isActive: Boolean,
    /**
     * Which configured instance of a multi-instance source (e.g. the second Navidrome server) this is.
     * Empty for single-instance sources and for an extension's default instance, so existing JSON,
     * DB rows and pref keys are unchanged. Instances other than the default also carry it in
     * [sourceName] (`<name>#<instanceId>`), which keeps every sourceName-keyed store isolated.
     */
    open val instanceId: String = "",
) {

    constructor(
        sourceName: String,
        sourceURL: String
    ) : this(
        sourceName = sourceName,
        sourceURL = sourceURL,
        isActive = true
    )

    @kotlin.jvm.JvmOverloads
    open fun copy(
        sourceName: String = this.sourceName,
        sourceURL: String = this.sourceURL,
        isActive: Boolean = this.isActive,
        instanceId: String = this.instanceId,
    ): SourceModel {
        return SourceModel(
            sourceName = sourceName,
            sourceURL = sourceURL,
            isActive = isActive,
            instanceId = instanceId,
        )
    }

    override fun toString(): String {
        return "SourceModel(sourceName=$sourceName, sourceURL=$sourceURL, isActive=$isActive, instanceId=$instanceId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourceModel) return false
        if (sourceName != other.sourceName) return false
        if (sourceURL != other.sourceURL) return false
        if (isActive != other.isActive) return false
        if (instanceId != other.instanceId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = sourceName.hashCode()
        result = 31 * result + sourceURL.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + instanceId.hashCode()
        return result
    }

    companion object {
        /** `<base>#<instanceId>` for a non-default instance, else [base]. */
        fun instanceSourceName(base: String, instanceId: String): String =
            if (instanceId.isEmpty()) base else "$base#$instanceId"

        /** The extension's own sourceName, without an instance suffix. */
        fun baseSourceName(sourceName: String): String = sourceName.substringBefore('#')

        @Suppress("FunctionName") // Factory method
        val LocalSource: SourceModel = SourceModel(
            sourceName = "Local",
            sourceURL = "::Local",
            isActive = true
        )
    }
}
