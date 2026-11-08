package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

/**
 * Host-provided local IPC: a unix domain socket, or a Windows named pipe. Bytes cross Zipline as base64.
 * Provider-neutral: the extension owns names, framing and protocol.
 */
interface LocalIpc : ZiplineService {
  /** Desktop true; Android / iOS false. */
  suspend fun isAvailable(): Boolean

  /**
   * Unix: tries `<dir>/<subdir>/<name>` for dir in XDG_RUNTIME_DIR, TMPDIR, TMP, TEMP, /tmp, and each of
   * [subdirs]. Windows: `\\.\pipe\<name>`. Returns a handle, or null when nothing is listening.
   */
  suspend fun connect(name: String, subdirs: List<String> = listOf("")): String?

  suspend fun write(handle: String, base64: String)

  /** Base64 of up to [maxBytes]; null on timeout. */
  suspend fun read(handle: String, maxBytes: Int, timeoutMs: Long): String?

  suspend fun close(handle: String)
}

/** Old hosts and platforms without local IPC. */
object UnavailableLocalIpc : LocalIpc {
  override suspend fun isAvailable(): Boolean = false
  override suspend fun connect(name: String, subdirs: List<String>): String? = null
  override suspend fun write(handle: String, base64: String) = throw UnsupportedOperationException("No local IPC")
  override suspend fun read(handle: String, maxBytes: Int, timeoutMs: Long): String? = null
  override suspend fun close(handle: String) {}
}

/** Host-provided WebSocket client (text frames). */
interface WebSocketClient : ZiplineService {
  suspend fun isAvailable(): Boolean

  /** Returns a handle; throws when the connection fails. */
  suspend fun open(url: String, headers: Map<String, String> = emptyMap()): String

  suspend fun send(handle: String, text: String)

  /** Next text frame; null on timeout. Throws once the socket is closed. */
  suspend fun receive(handle: String, timeoutMs: Long): String?

  suspend fun close(handle: String, code: Int = 1000)
}

object UnavailableWebSocket : WebSocketClient {
  override suspend fun isAvailable(): Boolean = false
  override suspend fun open(url: String, headers: Map<String, String>): String = throw UnsupportedOperationException("No WebSocket client")
  override suspend fun send(handle: String, text: String) = throw UnsupportedOperationException("No WebSocket client")
  override suspend fun receive(handle: String, timeoutMs: Long): String? = null
  override suspend fun close(handle: String, code: Int) {}
}
