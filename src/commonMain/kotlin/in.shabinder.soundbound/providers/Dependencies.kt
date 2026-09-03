package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.utils.DevicePreferences
import `in`.shabinder.soundbound.zipline.Crypto
import `in`.shabinder.soundbound.zipline.FuzzySearch
import `in`.shabinder.soundbound.zipline.HttpClientBuilder
import `in`.shabinder.soundbound.zipline.LocaleProvider
import `in`.shabinder.soundbound.zipline.RuntimeAssets
import `in`.shabinder.soundbound.zipline.SoundboundLogger
import `in`.shabinder.soundbound.zipline.HeadlessBrowser
import `in`.shabinder.soundbound.zipline.UnavailableRuntimeAssets

@Immutable
interface Dependencies {
  val devicePreferences: DevicePreferences
  val localeProvider: LocaleProvider
  val httpClientBuilder: HttpClientBuilder
  val fuzzySearch: FuzzySearch
  val logger: SoundboundLogger

  /**
   * A real browser engine, when the platform has one. Extensions should check
   * [HeadlessBrowser.capabilities] and degrade when it is unavailable rather than assume.
   *
   * This replaced `YTExtractor`, which named one provider's
   * concepts and therefore froze them at app-release cadence. New work belongs here.
   */
  val headlessBrowser: HeadlessBrowser
  /** Large app-bundled assets fetched only when an extension actually needs them. */
  val runtimeAssets: RuntimeAssets
    get() = UnavailableRuntimeAssets
  val crypto: Crypto
}
