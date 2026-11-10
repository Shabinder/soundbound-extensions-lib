package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.utils.GlobalJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Remote configuration for an extension: a small JSON object the extension's maintainers can change without shipping
 * a new bundle (endpoints, ids, pacing, a kill switch). An extension picks a namespace (usually its short name) and
 * reads `"$baseUrl$namespace.json"` (default [DEFAULT_BASE_URL]).
 *
 * The host fetches and caches it ([CachingRemoteConfig]). Use [values] for typed access; `"enabled": false` is the kill
 * switch ([RemoteConfigValues.enabled]). An unreachable or missing document never disables anything by itself.
 */
interface RemoteConfig : ZiplineService {
  /** The JSON object for [namespace], or null when there is none and nothing is cached. Never throws for network errors. */
  suspend fun document(namespace: String): String?

  companion object {
    const val DEFAULT_BASE_URL = "https://soundbound.app/config/sources/"
    private val NAMESPACE = Regex("""^[a-z0-9][a-z0-9._-]{0,63}$""")

    /** Lowercase letters, digits, `.`, `_`, `-`; at most 64 chars, no leading separator (it becomes a URL path and a file name). */
    fun isValidNamespace(namespace: String): Boolean = NAMESPACE.matches(namespace) && ".." !in namespace
  }
}

/** Hosts without remote config (tests, old hosts). */
object UnavailableRemoteConfig : RemoteConfig {
  override suspend fun document(namespace: String): String? = null
}

suspend fun RemoteConfig.values(namespace: String): RemoteConfigValues = RemoteConfigValues.parse(document(namespace))

/** Typed view over a remote config document. Missing keys and wrong types read as null / the default. */
class RemoteConfigValues(val json: JsonObject) {
  /** The kill switch: false only when the document says `"enabled": false`. */
  val enabled: Boolean get() = bool("enabled") ?: true

  fun string(key: String): String? = (json[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
  fun long(key: String): Long? = (json[key] as? JsonPrimitive)?.longOrNull
  fun bool(key: String): Boolean? = (json[key] as? JsonPrimitive)?.booleanOrNull
  fun strings(key: String): List<String> =
    (json[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }.orEmpty()

  /** The whole document as [serializer]'s type (unknown keys ignored); null when it does not fit. */
  fun <T> decode(serializer: KSerializer<T>): T? = runCatching { GlobalJson.decodeFromJsonElement(serializer, json) }.getOrNull()

  companion object {
    val EMPTY = RemoteConfigValues(JsonObject(emptyMap()))
    fun parse(document: String?): RemoteConfigValues =
      document?.let { runCatching { GlobalJson.parseToJsonElement(it) as? JsonObject }.getOrNull() }?.let(::RemoteConfigValues) ?: EMPTY
  }
}

/**
 * The cache every [RemoteConfig] host shares: fresh for [ttlMs] (6 h), then served stale while one refresh runs
 * ([revalidate] launches it in the background; without it the refresh runs inline). A failed fetch keeps the last good
 * document and is not retried for [retryMs]. `If-None-Match` is sent when the last answer had an ETag. A 404 means "no
 * document" until [retryMs] passes, so a newly published one is picked up quickly. Only a JSON object is accepted.
 *
 * Signatures are not verified: documents come over HTTPS from [RemoteConfig.DEFAULT_BASE_URL]'s host.
 *
 * ponytail: one mutex for every namespace; per-namespace locks if many extensions refresh at once.
 */
class CachingRemoteConfig(
  private val fetch: suspend (url: String, etag: String?) -> Response,
  private val store: Store,
  private val now: () -> Long,
  private val baseUrl: String = RemoteConfig.DEFAULT_BASE_URL,
  private val ttlMs: Long = 6 * 3_600_000L,
  private val retryMs: Long = 5 * 60_000L,
  private val revalidate: ((suspend () -> Unit) -> Unit)? = null,
) : RemoteConfig {

  /** [status] 304 = unchanged. */
  class Response(val status: Int, val body: String = "", val etag: String? = null)

  /** One string per namespace; the host decides where (file, preferences). */
  interface Store {
    fun read(namespace: String): String?
    fun write(namespace: String, value: String)
  }

  @Serializable
  internal data class Entry(val body: String? = null, val etag: String? = null, val fetchedAt: Long = 0, val retryAt: Long = 0)

  private val mutex = Mutex()
  private val memory = HashMap<String, Entry>()

  override suspend fun document(namespace: String): String? {
    if (!RemoteConfig.isValidNamespace(namespace)) return null
    val entry = mutex.withLock { entry(namespace) }
    val t = now()
    if (t - entry.fetchedAt < ttlMs || t < entry.retryAt) return entry.body
    val bg = revalidate
    if (bg != null && entry.body != null) {
      bg { refresh(namespace) }
      return entry.body
    }
    return refresh(namespace).body
  }

  private suspend fun refresh(namespace: String): Entry = mutex.withLock {
    val old = entry(namespace)
    // Another caller refreshed while this one waited.
    if (now() - old.fetchedAt < ttlMs || now() < old.retryAt) return old
    val r = try {
      fetch("$baseUrl$namespace.json", old.etag.takeIf { old.body != null })
    } catch (c: CancellationException) {
      throw c
    } catch (_: Throwable) {
      null
    }
    val t = now()
    val next = when {
      r == null -> old.copy(retryAt = t + retryMs)
      r.status == 304 -> old.copy(fetchedAt = t, retryAt = 0)
      r.status == 404 -> Entry(retryAt = t + retryMs) // no document (yet): asked again after the retry window, not the TTL
      r.status in 200..299 && isObject(r.body) -> Entry(r.body, r.etag, fetchedAt = t)
      else -> old.copy(retryAt = t + retryMs)
    }
    memory[namespace] = next
    runCatching { store.write(namespace, GlobalJson.encodeToString(Entry.serializer(), next)) }
    next
  }

  private fun entry(namespace: String): Entry = memory.getOrPut(namespace) {
    runCatching { store.read(namespace)?.let { GlobalJson.decodeFromString(Entry.serializer(), it) } }.getOrNull() ?: Entry()
  }

  private fun isObject(body: String) = runCatching { GlobalJson.parseToJsonElement(body) is JsonObject }.getOrDefault(false)
}
