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
  fun appendDiagnostics(key: String, value: Map<String, String?>): DiagnosticManager {
    data.add(DiagnosticsData(key, value))
    return this
  }

  fun appendDiagnostics(data: DiagnosticsData): DiagnosticManager {
    this.data.add(data)
    return this
  }

  fun getDiagnostics(): List<DiagnosticsData> = data

  override fun toString(): String = data.joinToString("\n") { "${it.key}: ${it.value}" }
}
