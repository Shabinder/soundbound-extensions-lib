@file:Suppress("NOTHING_TO_INLINE")

package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.ThrowableWrapper
import `in`.shabinder.soundbound.models.toThrowableWrapper
import `in`.shabinder.soundbound.zipline.SoundboundLogger.*
import kotlinx.serialization.Serializable

interface SoundboundLogger: ZiplineService  {
    @Serializable
    enum class Severity {
        Verbose,
        Debug,
        Info,
        Warn,
        Error,
        Assert
    }

    var tag: String

    fun log(
        severity: Severity,
        tag: String,
        throwable: ThrowableWrapper?,
        message: String
    )
}


fun SoundboundLogger.v(throwable: Throwable? = null, tag: String = this.tag, message: () -> String) {
    log(Severity.Verbose, tag, throwable?.toThrowableWrapper(), message())
}

fun SoundboundLogger.d(throwable: Throwable? = null, tag: String = this.tag, message: () -> String) {
    log(Severity.Debug, tag, throwable?.toThrowableWrapper(), message())
}

fun SoundboundLogger.i(throwable: Throwable? = null, tag: String = this.tag, message: () -> String) {
    log(Severity.Info, tag, throwable?.toThrowableWrapper(), message())
}

fun SoundboundLogger.w(throwable: Throwable? = null, tag: String = this.tag, message: () -> String) {
    log(Severity.Warn, tag, throwable?.toThrowableWrapper(), message())
}

fun SoundboundLogger.e(throwable: Throwable? = null, tag: String = this.tag, message: () -> String) {
    log(Severity.Error, tag, throwable?.toThrowableWrapper(), message())
}

fun SoundboundLogger.a(throwable: Throwable? = null, tag: String = this.tag, message: () -> String) {
    log(Severity.Assert, tag, throwable?.toThrowableWrapper(), message())
}

inline fun SoundboundLogger.v(messageString: String, throwable: Throwable? = null, tag: String = this.tag) {
    log(Severity.Verbose, tag, throwable?.toThrowableWrapper(), messageString)
}

inline fun SoundboundLogger.d(messageString: String, throwable: Throwable? = null, tag: String = this.tag) {
    log(Severity.Debug, tag, throwable?.toThrowableWrapper(), messageString)
}

inline fun SoundboundLogger.i(messageString: String, throwable: Throwable? = null, tag: String = this.tag) {
    log(Severity.Info, tag, throwable?.toThrowableWrapper(), messageString)
}

inline fun SoundboundLogger.w(messageString: String, throwable: Throwable? = null, tag: String = this.tag) {
    log(Severity.Warn, tag, throwable?.toThrowableWrapper(), messageString)
}

inline fun SoundboundLogger.e(messageString: String, throwable: Throwable? = null, tag: String = this.tag) {
    log(Severity.Error, tag, throwable?.toThrowableWrapper(), messageString)
}

inline fun SoundboundLogger.a(messageString: String, throwable: Throwable? = null, tag: String = this.tag) {
    log(Severity.Assert, tag, throwable?.toThrowableWrapper(), messageString)
}
