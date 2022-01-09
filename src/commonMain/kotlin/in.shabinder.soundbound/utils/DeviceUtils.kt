package `in`.shabinder.soundbound.utils

import `in`.shabinder.soundbound.models.AudioFormat
import `in`.shabinder.soundbound.models.AudioQuality

interface DeviceUtils {

    val preferredAudioQuality: AudioQuality

    val preferredAudioFormat: AudioFormat

    /*
    * Returns TRUE if file at absolutePath is present else false
    * */
    fun isPresent(absolutePath: String): Boolean

    /*
    * Core App will handle creating output path from:
    *  - source
    *  - itemName
    *  - itemIdentifiers <optional> , usually will be a single ID from Source
    * */
    fun finalOutputDir(
        source: String,
        itemName: String,
        vararg itemID: String? = emptyArray(),
    ): String

    /*
    * Get Image absolutePath from Image Link
    * */
    fun getImageCachePath(url: String): String
}