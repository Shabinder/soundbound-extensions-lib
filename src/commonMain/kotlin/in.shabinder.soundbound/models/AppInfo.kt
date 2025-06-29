package `in`.shabinder.soundbound.models

import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
  val packageId: String,
  val versionCode: Int,
  val platform: Platform,
)

@Serializable
enum class Platform {
  ANDROID,
  DESKTOP,
  IOS,
  WEB,
}
