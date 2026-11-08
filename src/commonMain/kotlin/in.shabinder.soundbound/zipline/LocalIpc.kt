package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

/**
 * A local inter-process channel: a unix domain socket, or a Windows named pipe. For talking to another app running on
 * the same machine (a chat client's RPC socket, a media daemon). Bytes cross Zipline as base64.
 *
 * Desktop only; Android and iOS report [isAvailable] false. The host gates it per extension (the user can switch it
 * off) and only accepts plain socket names, so an extension cannot reach arbitrary paths.
 */
interface LocalIpc : ZiplineService {
  suspend fun isAvailable(): Boolean

  /**
   * Unix: tries `<dir>/<subdir>/<name>` for dir in `XDG_RUNTIME_DIR`, `TMPDIR`, `TMP`, `TEMP`, `/tmp`, each subdir in
   * order. Windows: `\\.\pipe\<name>` (subdirs ignored). [name] and [subdirs] are plain relative names (letters,
   * digits, `.`, `_`, `-`, `/` between parts; no `..`). Returns a handle, or null when nothing is listening.
   */
  suspend fun connect(name: String, subdirs: List<String> = listOf("")): String?

  suspend fun write(handle: String, base64: String)

  /** Up to [maxBytes] bytes as base64, or null when nothing arrived within [timeoutMs]. Throws once the peer closed. */
  suspend fun read(handle: String, maxBytes: Int, timeoutMs: Long): String?

  /** Safe to call twice; unknown handles are ignored. */
  suspend fun close(handle: String)

  /** The host process id, for peers that tie state to the calling process (RPC servers clear it when it exits). */
  suspend fun processId(): Long
}

/** Used by hosts without local IPC (Android, iOS, tests, old hosts). */
object UnavailableLocalIpc : LocalIpc {
  override suspend fun isAvailable(): Boolean = false
  override suspend fun connect(name: String, subdirs: List<String>): String? = null
  override suspend fun write(handle: String, base64: String) = throw UnsupportedOperationException("no local IPC")
  override suspend fun read(handle: String, maxBytes: Int, timeoutMs: Long): String? = throw UnsupportedOperationException("no local IPC")
  override suspend fun close(handle: String) = Unit
  override suspend fun processId(): Long = 0L
}
