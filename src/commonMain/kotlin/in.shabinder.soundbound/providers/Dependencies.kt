package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.utils.DevicePreferences
import `in`.shabinder.soundbound.zipline.Crypto
import `in`.shabinder.soundbound.zipline.FuzzySearch
import `in`.shabinder.soundbound.zipline.HttpClientBuilder
import `in`.shabinder.soundbound.zipline.LocaleProvider
import `in`.shabinder.soundbound.zipline.SoundboundLogger
import `in`.shabinder.soundbound.zipline.HeadlessBrowser
import `in`.shabinder.soundbound.zipline.YTExtractor

@Immutable
interface Dependencies {
  val devicePreferences: DevicePreferences
  val localeProvider: LocaleProvider
  val httpClientBuilder: HttpClientBuilder
  val fuzzySearch: FuzzySearch
  val logger: SoundboundLogger
  val ytExtractor: YTExtractor

  /**
   * A real browser engine, when the platform has one. Extensions should check
   * [HeadlessBrowser.capabilities] and degrade when it is unavailable rather than assume.
   *
   * This is the general-purpose replacement for [ytExtractor], which names one provider's
   * concepts and therefore froze them at app-release cadence. New work belongs here.
   */
  val headlessBrowser: HeadlessBrowser
  val crypto: Crypto
}
