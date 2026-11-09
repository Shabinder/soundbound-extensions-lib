package `in`.shabinder.soundbound.models

import `in`.shabinder.soundbound.utils.GlobalJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SourceModelInstanceTest {
  @Test fun defaultInstanceKeepsTodaysName() {
    assertEquals("x.Navidrome", SourceModel.instanceSourceName("x.Navidrome", ""))
    assertEquals("x.Navidrome#home", SourceModel.instanceSourceName("x.Navidrome", "home"))
    assertEquals("x.Navidrome", SourceModel.baseSourceName("x.Navidrome#home"))
  }

  @Test fun instancesAreDistinctAndOldJsonStillParses() {
    val a = SourceModel("n", "u", true)
    val b = a.copy(sourceName = "n#2", instanceId = "2")
    assertNotEquals(a, b)
    assertEquals(a, GlobalJson.decodeFromString<SourceModel>("""{"sourceName":"n","sourceURL":"u","isActive":true}"""))
    assertEquals(b, GlobalJson.decodeFromString<SourceModel>(GlobalJson.encodeToString(SourceModel.serializer(), b)))
  }
}
