package `in`.shabinder.soundbound.utils

import androidx.compose.runtime.Immutable
import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.AudioFormat
import `in`.shabinder.soundbound.models.AudioQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer


@Immutable
interface DevicePreferences : ZiplineService {
  val preferredAudioQuality: AudioQuality

  val preferredAudioFormat: AudioFormat

  fun getSystemTimeMillis(): Long

  fun getTimeZoneId(): String

  fun getStringOrNull(key: String): String?
  fun putString(key: String, value: String)


  fun getIntOrNull(key: String): Int?
  fun putInt(key: String, value: Int)

  fun getBooleanOrNull(key: String): Boolean?
  fun putBoolean(key: String, value: Boolean)

  fun getLongOrNull(key: String): Long?
  fun putLong(key: String, value: Long)

  fun getFloatOrNull(key: String): Float?
  fun putFloat(key: String, value: Float)

  fun getIntFlow(key: String, defaultValue: Int): Flow<Int>
  fun getLongFlow(key: String, defaultValue: Long): Flow<Long>
  fun getFloatFlow(key: String, defaultValue: Float): Flow<Float>
  fun getStringFlow(key: String, defaultValue: String): Flow<String>
  fun getBooleanFlow(key: String, defaultValue: Boolean): Flow<Boolean>

  fun remove(key: String)

  fun clear()
}


/************************  HELPERS  ************************/
inline fun <reified T> DevicePreferences.getSerializedOrNull(key: String): T? {
  return getSerializedOrNull(key, serializer())
}

inline fun <reified T> DevicePreferences.putSerializedString(key: String, value: T) {
  putSerializedString(key, value, serializer())
}

inline fun <reified T> DevicePreferences.getSerializedFlow(key: String, defaultValue: T): Flow<T> {
  return getSerializedFlow(key, defaultValue, serializer())
}

inline fun <reified T> DevicePreferences.getSerializedOrNullFlow(key: String, defaultValue: T): Flow<T?> {
  return getSerializedOrNullFlow(key, defaultValue, serializer())
}

fun <T> DevicePreferences.getSerializedOrNull(key: String, serializer: KSerializer<T>): T? =
  runCatching {
    getStringOrNull(key)?.let { GlobalJson.decodeFromString(serializer, it) }
  }.onFailure {
    it.printStackTrace()
  }.getOrNull()

fun <T> DevicePreferences.getSerializedFlow(
  key: String,
  defaultValue: T,
  serializer: KSerializer<T>
): Flow<T> {
  return getStringFlow(key, GlobalJson.encodeToString(serializer, defaultValue))
    .map { s -> GlobalJson.decodeFromString(serializer, s) }
}

fun <T> DevicePreferences.getSerializedOrNullFlow(
  key: String,
  defaultValue: T?,
  serializer: KSerializer<T>
): Flow<T?> {
  val nullableDefaultValue = defaultValue?.let {
    runCatching { GlobalJson.encodeToString(serializer, defaultValue) }
      .getOrNull()
  }


  return getStringFlow(key, nullableDefaultValue ?: "")
    .map { s ->
      if (s.isEmpty()) {
        null
      } else {
        runCatching {
          GlobalJson.decodeFromString(serializer, s)
        }.getOrNull()
      }
    }
}

fun <T> DevicePreferences.putSerializedString(key: String, value: T, serializer: KSerializer<T>) {
  putString(key, GlobalJson.encodeToString(serializer, value))
}
