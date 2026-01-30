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

  companion object {
    /**
     * Generates a curl command string from HTTP request details for easy reproduction.
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param url The request URL
     * @param headers Request headers as a map
     * @param params Query parameters as a map
     * @param body Request body (for POST/PUT requests)
     * @param bodyType Type of body content (JSON, FORM, RAW)
     * @return A formatted curl command string
     */
    fun buildCurlCommand(
      method: String,
      url: String,
      headers: Map<String, String> = emptyMap(),
      params: Map<String, String> = emptyMap(),
      body: String? = null,
      bodyType: String? = null
    ): String = buildString {
      append("curl -X $method")

      // Add headers
      headers.forEach { (key, value) ->
        append(" \\\n  -H '${escapeForShell(key)}: ${escapeForShell(value)}'")
      }

      // Add content-type for body if not already specified
      if (body != null && !headers.keys.any { it.equals("Content-Type", ignoreCase = true) }) {
        val contentType = when (bodyType?.uppercase()) {
          "JSON" -> "application/json"
          "FORM" -> "application/x-www-form-urlencoded"
          else -> "text/plain"
        }
        append(" \\\n  -H 'Content-Type: $contentType'")
      }

      // Add body data
      if (!body.isNullOrBlank()) {
        append(" \\\n  -d '${escapeForShell(body)}'")
      }

      // Build URL with query parameters
      val fullUrl = if (params.isNotEmpty()) {
        val queryString = params.entries.joinToString("&") { (k, v) ->
          "${encodeUrlParam(k)}=${encodeUrlParam(v)}"
        }
        if (url.contains("?")) "$url&$queryString" else "$url?$queryString"
      } else {
        url
      }

      append(" \\\n  '${escapeForShell(fullUrl)}'")
    }

    private fun escapeForShell(value: String): String =
      value.replace("'", "'\\''")

    private fun encodeUrlParam(value: String): String =
      value.replace(" ", "%20")
        .replace("&", "%26")
        .replace("=", "%3D")
        .replace("?", "%3F")
        .replace("#", "%23")
  }
}
