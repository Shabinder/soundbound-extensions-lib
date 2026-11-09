package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

/**
 * Raw TCP for extensions that speak a non-HTTP protocol (peer-to-peer networks, chat/IRC, usenet NNTP, game or
 * media daemons). Pull-based like [LocalIpc] and [WebSocketClient]: the extension polls [read] instead of taking
 * callbacks across the bridge, which gives natural backpressure (nothing is read off the socket until asked). Bytes
 * cross Zipline as base64.
 *
 * The host gates it per extension behind a user-visible switch, caps open sockets and listeners, and refuses
 * loopback / private / link-local targets unless the extension's user setting allows the local network, so a bundle
 * cannot scan the user's LAN. [capabilities] says what this host allows right now; check it and degrade.
 *
 * Mobile notes: sockets live only while the app process is allowed to run (Android: foreground or a foreground
 * service; iOS: foreground). [listen] needs a reachable port, which carrier NAT usually prevents; extensions should
 * prefer outbound connections and treat inbound as a bonus.
 */
interface NetworkSockets : ZiplineService {
  suspend fun capabilities(): SocketCapabilities

  /** Opens a TCP connection; returns a handle. Throws when it cannot connect within [timeoutMs]. */
  suspend fun connect(host: String, port: Int, timeoutMs: Long): String

  /** Writes all of [base64]'s bytes; suspends while the socket's send buffer is full. */
  suspend fun write(handle: String, base64: String)

  /**
   * Up to [maxBytes] bytes as base64, or null when nothing arrived within [timeoutMs]. Throws once the peer closed
   * and every buffered byte was read.
   */
  suspend fun read(handle: String, maxBytes: Int, timeoutMs: Long): String?

  /** Safe to call twice; unknown handles are ignored. Closes listeners too. */
  suspend fun close(handle: String)

  /**
   * Listens on [port] (0 = any free port) on all interfaces. Returns a listener handle; [localPort] gives the bound
   * port. Throws when [SocketCapabilities.listen] is false.
   */
  suspend fun listen(port: Int): String

  /** The bound port of a listener handle. */
  suspend fun localPort(listener: String): Int

  /** The next inbound connection on [listener], or null when none arrived within [timeoutMs]. */
  suspend fun accept(listener: String, timeoutMs: Long): AcceptedSocket?
}

@Serializable
data class SocketCapabilities(
  val connect: Boolean = false,
  val listen: Boolean = false,
  /** Loopback / RFC 1918 / link-local targets allowed (the user opted in for this extension). */
  val localNetwork: Boolean = false,
  val maxOpen: Int = 0,
)

@Serializable
data class AcceptedSocket(val handle: String, val remoteHost: String, val remotePort: Int)

/** Used by hosts without the capability (tests, old hosts), and by the host when the user has not allowed it. */
object UnavailableNetworkSockets : NetworkSockets {
  override suspend fun capabilities(): SocketCapabilities = SocketCapabilities()
  override suspend fun connect(host: String, port: Int, timeoutMs: Long): String = throw UnsupportedOperationException("no network sockets")
  override suspend fun write(handle: String, base64: String) = throw UnsupportedOperationException("no network sockets")
  override suspend fun read(handle: String, maxBytes: Int, timeoutMs: Long): String? = throw UnsupportedOperationException("no network sockets")
  override suspend fun close(handle: String) = Unit
  override suspend fun listen(port: Int): String = throw UnsupportedOperationException("no network sockets")
  override suspend fun localPort(listener: String): Int = 0
  override suspend fun accept(listener: String, timeoutMs: Long): AcceptedSocket? = null
}
