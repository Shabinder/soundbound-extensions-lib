package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

/**
 * A text WebSocket client. Pull-based like [HeadlessBrowser]: the extension polls [receive] instead of taking
 * callbacks across the bridge, which keeps a slow consumer safe and maps onto every client library.
 */
interface WebSocketClient : ZiplineService {
  suspend fun isAvailable(): Boolean

  /** Connects and returns a handle. Throws when the handshake fails. */
  suspend fun open(url: String, headers: Map<String, String> = emptyMap()): String

  suspend fun send(handle: String, text: String)

  /** The next text message, or null when none arrived within [timeoutMs]. Throws once the socket closed. */
  suspend fun receive(handle: String, timeoutMs: Long): String?

  /** Safe to call twice; unknown handles are ignored. */
  suspend fun close(handle: String, code: Int = 1000)
}

/** Used by hosts without WebSocket support (iOS today, tests, old hosts). */
object UnavailableWebSocket : WebSocketClient {
  override suspend fun isAvailable(): Boolean = false
  override suspend fun open(url: String, headers: Map<String, String>): String = throw UnsupportedOperationException("no WebSocket")
  override suspend fun send(handle: String, text: String) = throw UnsupportedOperationException("no WebSocket")
  override suspend fun receive(handle: String, timeoutMs: Long): String? = throw UnsupportedOperationException("no WebSocket")
  override suspend fun close(handle: String, code: Int) = Unit
}
