package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable


@Immutable
@Serializable
sealed class DownloadStatus {


    @Immutable
    @Serializable
    data object Downloaded : DownloadStatus()


    @Immutable
    @Serializable
    data class Downloading(val progress: Int = 2) : DownloadStatus()


    @Immutable
    @Serializable
    data object Queued : DownloadStatus()


    @Immutable
    @Serializable
    data object NotDownloaded : DownloadStatus()


    @Immutable
    @Serializable
    data object Converting : DownloadStatus()


    @Immutable
    @Serializable
    sealed class Failed : DownloadStatus() {

        companion object {
            fun error(error: Throwable) = Error(error.toThrowableWrapper())
        }

        @Serializable

        @Immutable
        data class Error<T : ThrowableWrapper>(val errors: List<T>) : Failed() {
            constructor(error: T) : this(listOf(error))

            @Suppress("UNCHECKED_CAST")
            constructor(error: Throwable) : this(errors = listOf<T>(error.toThrowableWrapper() as T))
        }


        @Serializable

        @Immutable
        data class ProviderErrors(val errors: Map<SourceModel, Error<*>>) : Failed()

        val allErrors: List<ThrowableWrapper>
            get() = when (this) {
                is Error<*> -> errors
                is ProviderErrors -> errors.values.map { it.errors }.flatten()
            }
    }
}
