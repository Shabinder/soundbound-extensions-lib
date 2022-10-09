package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.utils.Context
import `in`.shabinder.soundbound.utils.DevicePreferences

interface Dependencies {
    var appContext: Context
    var devicePreferences: DevicePreferences
}
