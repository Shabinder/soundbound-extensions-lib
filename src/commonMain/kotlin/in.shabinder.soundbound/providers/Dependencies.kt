package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.utils.DevicePreferences
import `in`.shabinder.soundbound.zipline.Crypto
import `in`.shabinder.soundbound.zipline.FuzzySearch
import `in`.shabinder.soundbound.zipline.HttpClientBuilder
import `in`.shabinder.soundbound.zipline.LocaleProvider
import `in`.shabinder.soundbound.zipline.SoundboundLogger
import `in`.shabinder.soundbound.zipline.YTExtractor

@Immutable
interface Dependencies {
    val devicePreferences: DevicePreferences
    val localeProvider: LocaleProvider
    val httpClientBuilder: HttpClientBuilder
    val fuzzySearch: FuzzySearch
    val crypto: Crypto
    val ytExtractor: YTExtractor
    val logger: SoundboundLogger
}
