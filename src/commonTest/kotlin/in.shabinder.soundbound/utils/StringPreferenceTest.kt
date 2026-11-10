package `in`.shabinder.soundbound.utils

import `in`.shabinder.soundbound.models.AudioFormat
import `in`.shabinder.soundbound.models.AudioQuality
import kotlinx.coroutines.flow.Flow
import kotlin.test.Test
import kotlin.test.assertEquals

class StringPreferenceTest {
  private class MapPrefs : DevicePreferences {
    val m = HashMap<String, String>()
    override val preferredAudioQuality get() = error("unused")
    override val preferredAudioFormat: AudioFormat get() = error("unused")
    override fun getSystemTimeMillis() = 0L
    override fun getTimeZoneId() = "UTC"
    override fun getStringOrNull(key: String) = m[key]
    override fun putString(key: String, value: String) { m[key] = value }
    override fun getIntOrNull(key: String): Int? = null
    override fun putInt(key: String, value: Int) = Unit
    override fun getBooleanOrNull(key: String): Boolean? = null
    override fun putBoolean(key: String, value: Boolean) = Unit
    override fun getLongOrNull(key: String): Long? = null
    override fun putLong(key: String, value: Long) = Unit
    override fun getFloatOrNull(key: String): Float? = null
    override fun putFloat(key: String, value: Float) = Unit
    override fun getIntFlow(key: String, defaultValue: Int): Flow<Int> = error("unused")
    override fun getLongFlow(key: String, defaultValue: Long): Flow<Long> = error("unused")
    override fun getFloatFlow(key: String, defaultValue: Float): Flow<Float> = error("unused")
    override fun getStringFlow(key: String, defaultValue: String): Flow<String> = error("unused")
    override fun getBooleanFlow(key: String, defaultValue: Boolean): Flow<Boolean> = error("unused")
    override fun remove(key: String) { m.remove(key) }
    override fun clear() = m.clear()
  }

  @Test
  fun writtenStringReadsBackWithoutJsonQuotes() {
    val prefs = MapPrefs()
    propWithFlow(key = "countryCode", defaultValue = "US", devicePreferences = prefs).value = "IN"
    // A fresh delegate reads what was stored (`"IN"`, JSON): it must be IN, never "IN" with its quotes.
    val read = propWithFlow(key = "countryCode", defaultValue = "US", devicePreferences = prefs).value
    assertEquals("IN", read)
    assertEquals("https://music.apple.com/in/song/1", "https://music.apple.com/${read.lowercase()}/song/1")
  }

  @Test
  fun rawStringStaysAsStored() {
    val prefs = MapPrefs()
    prefs.putString("id", "f413f37797ec407f")
    prefs.putString("url", "http://localhost:4534")
    assertEquals("f413f37797ec407f", prefs.getSerializedOrNull<String>("id"))
    assertEquals("http://localhost:4534", prefs.getSerializedOrNull<String>("url"))
  }
}
