package `in`.shabinder.soundbound.utils

import android.content.Context

actual typealias Context = AppContextProvider

open class AppContextProvider(val appContext: Context)