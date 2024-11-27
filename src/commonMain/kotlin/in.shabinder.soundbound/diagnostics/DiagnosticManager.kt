package `in`.shabinder.soundbound.diagnostics

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class DiagnosticsData(val key: String, val value: Map<String, String?>)

@Immutable
@Serializable
data class DiagnosticManager(
  private val data: MutableList<DiagnosticsData> = mutableListOf()
) {
  fun appendDiagnostics(key: String, value: Map<String, String?>): DiagnosticManager = apply {
    data.add(DiagnosticsData(key, value))
  }

  fun appendDiagnostics(
    key: String,
    error: Throwable,
    extraValues: Map<String, String?> = emptyMap()
  ): DiagnosticManager = apply {
    data.add(
      DiagnosticsData(
        key = key,
        value = mapOf(
          "message" to error.message,
          "stackTrace" to error.stackTraceToString(),
          "errorClass" to error::class.simpleName
        ) + extraValues
      )
    )
  }

  fun appendDiagnostics(data: DiagnosticsData): DiagnosticManager = apply {
    this.data.add(data)
  }

  fun getDiagnostics(): List<DiagnosticsData> = data

  override fun toString(): String = data.joinToString("\n") { "${it.key}: ${it.value}" }
}
