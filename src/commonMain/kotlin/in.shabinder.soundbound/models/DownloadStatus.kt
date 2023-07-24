package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Immutable
@Serializable
sealed class DownloadStatus : Parcelable {

    @Parcelize
    @Immutable
    @Serializable
    object Downloaded : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    data class Downloading(val progress: Int = 2) : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    object Queued : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    object NotDownloaded : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    object Converting : DownloadStatus()

    @Parcelize
    @Immutable
    @Serializable
    sealed class Failed : DownloadStatus() {

        @Serializable
        @Parcelize
        @Immutable
        data class Error(val errors: List<ThrowableWrapper>) : Failed() {
            constructor(error: ThrowableWrapper) : this(listOf(error))

            constructor(error: Throwable) : this(ThrowableWrapper(error))
        }


        @Serializable
        @Parcelize
        @Immutable
        data class ProviderErrors(val errors: Map<SourceModel, Error>) : Failed()

        val allErrors: List<ThrowableWrapper>
            get() = when (this) {
                is Error -> errors
                is ProviderErrors -> errors.values.map { it.errors }.flatten()
            }
    }
}
