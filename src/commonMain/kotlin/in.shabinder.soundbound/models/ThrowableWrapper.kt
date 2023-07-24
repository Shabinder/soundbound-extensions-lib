package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Parcelize
open class ThrowableWrapper(
    override val message: String,
    val stackTrace: String = "",
    override val cause: ThrowableWrapper? = null
) : Throwable(message, cause), Parcelable {
    constructor(throwable: Throwable) : this(
        message = throwable.message ?: "",
        stackTrace = throwable.stackTraceToString(),
        cause = throwable.cause?.toThrowableWrapper()
    )

    open fun toThrowable() = Throwable(message + "\n" + stackTrace)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThrowableWrapper) return false
        if (message != other.message) return false
        if (stackTrace != other.stackTrace) return false
        return true
    }

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + stackTrace.hashCode()
        return result
    }

    override fun toString(): String {
        return message + "-> " + toThrowable().stackTraceToString()
    }
}

fun Throwable.toThrowableWrapper() = if (this is ThrowableWrapper) this else ThrowableWrapper(this)