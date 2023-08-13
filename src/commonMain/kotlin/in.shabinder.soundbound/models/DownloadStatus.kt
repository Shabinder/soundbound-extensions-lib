package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.parcelize.Parcelable
import `in`.shabinder.soundbound.parcelize.Parcelize
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

        companion object {
            fun error(error: Throwable) = Error(error.toThrowableWrapper())
        }

        @Serializable
        @Parcelize
        @Immutable
        data class Error<T: ThrowableWrapper>(val errors: List<T>) : Failed() {
            constructor(error: T) : this(listOf(error))

            @Suppress("UNCHECKED_CAST")
            constructor(error: Throwable) : this(errors = listOf<T>(error.toThrowableWrapper() as T))
        }


        @Serializable
        @Parcelize
        @Immutable
        data class ProviderErrors(val errors: Map<SourceModel, Error<*>>) : Failed()

        val allErrors: List<ThrowableWrapper>
            get() = when (this) {
                is Error<*> -> errors
                is ProviderErrors -> errors.values.map { it.errors }.flatten()
            }
    }
}
