package `in`.shabinder.soundbound.zipline

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoteConfigTest {
  private class MapStore : CachingRemoteConfig.Store {
    val map = HashMap<String, String>()
    override fun read(namespace: String) = map[namespace]
    override fun write(namespace: String, value: String) { map[namespace] = value }
  }

  private var t = 1_000_000L
  private val calls = mutableListOf<Pair<String, String?>>()
  private var answer: () -> CachingRemoteConfig.Response = { CachingRemoteConfig.Response(200, """{"enabled":true,"n":1}""", "\"e1\"") }
  private val store = MapStore()
  private fun config(revalidate: ((suspend () -> Unit) -> Unit)? = null) = CachingRemoteConfig(
    fetch = { url, etag -> calls += url to etag; answer() },
    store = store, now = { t }, ttlMs = 100, retryMs = 50, revalidate = revalidate,
  )

  @Test
  fun cachesForTtlThenRevalidatesWithEtag() = runTest {
    val c = config()
    assertEquals("""{"enabled":true,"n":1}""", c.document("x"))
    assertEquals("https://soundbound.app/config/sources/x.json" to null, calls.single())
    t += 50; c.document("x"); assertEquals(1, calls.size)
    t += 60; answer = { CachingRemoteConfig.Response(304) }
    assertEquals("""{"enabled":true,"n":1}""", c.document("x"))
    assertEquals("\"e1\"", calls.last().second)
    // Survives a restart through the store.
    assertEquals("""{"enabled":true,"n":1}""", config().document("x")); assertEquals(2, calls.size)
  }

  @Test
  fun failureKeepsLastGoodAndBacksOff() = runTest {
    val c = config(); c.document("x")
    t += 200; answer = { error("offline") }
    assertEquals("""{"enabled":true,"n":1}""", c.document("x")); assertEquals(2, calls.size)
    t += 10; c.document("x"); assertEquals(2, calls.size) // inside retry window
    t += 60; answer = { CachingRemoteConfig.Response(200, "<html>challenge</html>") }
    assertEquals("""{"enabled":true,"n":1}""", c.document("x")); assertEquals(3, calls.size)
  }

  @Test
  fun notFoundMeansNoDocumentAndStaleServesWhileRevalidating() = runTest {
    answer = { CachingRemoteConfig.Response(404) }
    assertNull(config().document("y"))
    val pending = mutableListOf<suspend () -> Unit>()
    answer = { CachingRemoteConfig.Response(200, """{"n":1}""") }
    val c = config { pending += it }
    t += 200
    assertEquals("""{"n":1}""", c.document("y")) // 404 cached nothing to serve stale, so this one is inline
    assertTrue(pending.isEmpty())
    answer = { CachingRemoteConfig.Response(200, """{"n":2}""") }
    t += 200
    assertEquals("""{"n":1}""", c.document("y")) // stale served
    pending.single()()
    assertEquals("""{"n":2}""", c.document("y"))
  }

  @Test
  fun badNamespaceNeverFetches() = runTest {
    val c = config()
    assertNull(c.document("../etc")); assertNull(c.document("A")); assertNull(c.document(""))
    assertTrue(calls.isEmpty())
  }

  @Test
  fun typedValues() {
    val v = RemoteConfigValues.parse("""{"enabled":false,"hosts":["a",1,"b"],"gap":20000,"name":"n","flag":"true"}""")
    assertFalse(v.enabled)
    assertEquals(listOf("a", "b"), v.strings("hosts"))
    assertEquals(20_000L, v.long("gap")); assertEquals("n", v.string("name")); assertNull(v.string("gap"))
    assertTrue(RemoteConfigValues.parse(null).enabled)
    assertTrue(RemoteConfigValues.parse("[1]").enabled)
  }
}
