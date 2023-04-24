package `in`.shabinder.soundbound.models

import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.serialization.Serializable

@Serializable
abstract class BaseDownloadableModel(): Parcelable {
    abstract val downloaded: DownloadStatus

    override fun toString() = "BaseDownloadableModel(downloadStatus=$downloaded)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseDownloadableModel) return false
        if (downloaded != other.downloaded) return false
        return true
    }

    override fun hashCode(): Int {
        return downloaded.hashCode()
    }
}