package `in`.shabinder.soundbound.utils

interface FileUtils {
    /*
    * Returns TRUE if file at absolutePath is present else false
    * */
    fun isPresent(absolutePath: String): Boolean

    /*
    * Core App will handle creating output path from:
    *  - itemName
    *  - itemID <optional>
    *  - source
    * */
    fun finalOutputDir(
        source: String,
        itemName: String,
        itemID: String? = null,
    ): String

    /*
    * Get Image absolutePath from Image Link
    * */
    fun getImageCachePath(url: String): String
}