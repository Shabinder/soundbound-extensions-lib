package `in`.shabinder.soundbound.diagnostics

import `in`.shabinder.soundbound.models.ProviderExceptions
import `in`.shabinder.soundbound.models.toThrowableWrapper
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext

// DiagnosticContext: A CoroutineContext Element to manage DiagnosticHelper
class DiagnosticContext private constructor(val manager: DiagnosticManager) :
  CoroutineContext.Element {

  override val key: CoroutineContext.Key<*> get() = Key

  companion object Key : CoroutineContext.Key<DiagnosticContext> {
    suspend fun <T> withDiagnostics(block: suspend DiagnosticManager.() -> T): T {
      val currentContext = coroutineContext[Key]
      return if (currentContext != null) {
        currentContext.manager.tryOrThrowWithDiagnostics { block() }
      } else {
        val newDiagnostics = DiagnosticManager()
        val newContext = DiagnosticContext(newDiagnostics)
        withContext(coroutineContext + newContext) {
          newDiagnostics.tryOrThrowWithDiagnostics { block() }
        }
      }
    }

    private inline fun <T> DiagnosticManager.tryOrThrowWithDiagnostics(
      block: DiagnosticManager.() -> T
    ): T = try {
      block()
    } catch (e: Throwable) {
      appendDiagnostics(
        "ErrorCaughtWithDiagnostics", mapOf(
          "message" to e.message,
          "stackTrace" to e.stackTraceToString(),
        )
      )
      throw ProviderExceptions.ExceptionWithDiagnostics(
        cause = e.toThrowableWrapper(),
        diagnostics = getDiagnostics()
      )
    }
  }
}
