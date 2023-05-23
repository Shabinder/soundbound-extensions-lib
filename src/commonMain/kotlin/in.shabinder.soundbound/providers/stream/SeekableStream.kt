package `in`.shabinder.soundbound.providers.stream

interface SeekableInputStream : Stream {
    fun seek(positionBytes: Long)
    fun tell(): Long
    fun length(): Long
}
