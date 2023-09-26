package `in`.shabinder.soundbound.utils

import androidx.compose.runtime.Immutable
import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.AudioFormat
import `in`.shabinder.soundbound.models.AudioQuality


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

    fun remove(key: String)

    fun clear()
}