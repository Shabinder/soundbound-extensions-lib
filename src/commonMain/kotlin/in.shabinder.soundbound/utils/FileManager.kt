package `in`.shabinder.soundbound.utils

interface FileManager {
    fun isPresent(filePath: String): Boolean
    fun delete(filePath: String): Boolean
    fun fileSeparator(): String
    fun defaultDir(): String
    fun cacheDir(): String
    fun createDirectory(dirPath: String)

    suspend fun clearCache()
}