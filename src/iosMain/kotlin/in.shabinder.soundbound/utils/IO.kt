package `in`.shabinder.soundbound.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Native has no Dispatchers.IO; Default is the standard fallback (mirrors jsMain).
actual val Dispatchers.IO: CoroutineDispatcher
    get() = Default
