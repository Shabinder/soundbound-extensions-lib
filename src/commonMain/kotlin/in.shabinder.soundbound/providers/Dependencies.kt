package `in`.shabinder.soundbound.providers

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.utils.DevicePreferences
import `in`.shabinder.soundbound.zipline.AccountLink
import `in`.shabinder.soundbound.zipline.Crypto
import `in`.shabinder.soundbound.zipline.FuzzySearch
import `in`.shabinder.soundbound.zipline.HttpClientBuilder
import `in`.shabinder.soundbound.zipline.LocaleProvider
import `in`.shabinder.soundbound.zipline.RuntimeAssets
import `in`.shabinder.soundbound.zipline.SoundboundLogger
import `in`.shabinder.soundbound.zipline.HeadlessBrowser
import `in`.shabinder.soundbound.zipline.UnavailableRuntimeAssets
import `in`.shabinder.soundbound.zipline.LocalIpc
import `in`.shabinder.soundbound.zipline.UnavailableAccountLink
import `in`.shabinder.soundbound.zipline.UnavailableLocalIpc
import `in`.shabinder.soundbound.zipline.UnavailableWebSocket
import `in`.shabinder.soundbound.zipline.WebSocketClient

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
  /** Unix socket / named pipe to another local app. Desktop only, gated per extension by the host. */
  val localIpc: LocalIpc
    get() = UnavailableLocalIpc
  /** Text WebSocket client. */
  val webSocket: WebSocketClient
    get() = UnavailableWebSocket
  /** OAuth2 account linking through the SoundBound backend, gated per extension by the host. */
  val accountLink: AccountLink
    get() = UnavailableAccountLink
  val crypto: Crypto
}
