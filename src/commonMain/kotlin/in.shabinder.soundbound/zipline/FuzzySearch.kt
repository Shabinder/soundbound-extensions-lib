package `in`.shabinder.soundbound.zipline

interface FuzzySearch {
    fun ratio(
        a: String,
        b: String,
    ): Float

    fun partialRatio(
        a: String,
        b: String,
    ): Float
}