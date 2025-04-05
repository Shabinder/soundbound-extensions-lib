package `in`.shabinder.soundbound.models

import kotlinx.serialization.Serializable

@Serializable
class PoTokenResult(
  val playerRequestPoToken: String,
  val streamingDataPoToken: String,
)
