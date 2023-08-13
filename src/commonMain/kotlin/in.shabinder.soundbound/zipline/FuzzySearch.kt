package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

interface FuzzySearch: ZiplineService {
    fun ratio(
        a: String,
        b: String,
    ): Float

    fun partialRatio(
        a: String,
        b: String,
    ): Float
}