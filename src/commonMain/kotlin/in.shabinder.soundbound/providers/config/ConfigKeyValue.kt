package `in`.shabinder.soundbound.providers.config

import kotlin.collections.List as KotlinList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

@Serializable
sealed class ConfigPropertyKey {
  abstract val key: String
  abstract val title: String
  abstract val description: String
  abstract val isRequired: Boolean
  abstract val isUserConfigurable: Boolean

  @Serializable
  data class Single(
    override val key: String,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean = false,
    override val isUserConfigurable: Boolean = false,
    val defaultValue: JsonPrimitive,
    val possibleValues: KotlinList<JsonPrimitive> = emptyList(), // Can be Empty, if require user to enter manually
    // How the app should render / store the value. Older apps ignore it (Zipline JSON drops
    // unknown keys) and show a plain text field, so TEXT is the safe default.
    val inputType: ConfigInputType = ConfigInputType.TEXT,
  ) : ConfigPropertyKey()

  @Serializable
  data class List(
    override val key: String,
    override val title: String,
    override val description: String,
    val isMultiSelection: Boolean = false,
    override val isRequired: Boolean = false,
    override val isUserConfigurable: Boolean = false,
    val defaultValue: JsonArray,
    val possibleValues: JsonArray = JsonArray(emptyList())
  ) : ConfigPropertyKey()
}

/** Rendering / storage hint for a [ConfigPropertyKey.Single] value. */
@Serializable
enum class ConfigInputType {
  TEXT,
  /** A server address (http/https). The app may validate it and allow LAN http. */
  URL,
  USERNAME,
  /** Password / API key / token: masked, never echoed, stored in the platform keystore where available. */
  SECRET,
  NUMBER,
  BOOLEAN,
}

@Serializable
sealed class ConfigPropertyValue() {
  abstract val key: ConfigPropertyKey

  open fun isDefault(): Boolean {
    return when (this) {
      is Single -> value == key.defaultValue
      is List -> value == key.defaultValue
    }
  }

  @Serializable
  data class Single(
    override val key: ConfigPropertyKey.Single,
    val value: JsonPrimitive
  ) : ConfigPropertyValue()

  @Serializable
  data class List(
    override val key: ConfigPropertyKey.List,
    val value: JsonArray
  ) : ConfigPropertyValue()
}

