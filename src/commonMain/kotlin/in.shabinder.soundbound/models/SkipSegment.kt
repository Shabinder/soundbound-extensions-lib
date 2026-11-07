package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * A span of a track the player may skip automatically (e.g. a non-music intro or outro).
 *
 * Provider-neutral: [category] and [actionType] are free-form labels chosen by the extension that
 * supplied the segment; the app treats every segment with [actionType] `"skip"` the same way.
 * [id] is the provider's stable id for the segment, used to skip each one at most once per play.
 */
@Immutable
@Serializable
data class SkipSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String,
    val actionType: String = ACTION_SKIP,
    val id: String,
) {
    companion object {
        const val ACTION_SKIP = "skip"
    }
}
