package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.utils.Context
import `in`.shabinder.soundbound.utils.DeviceUtils

interface Dependencies {
    var appContext: Context
    var deviceUtils: DeviceUtils
}
