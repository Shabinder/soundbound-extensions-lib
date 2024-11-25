package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.providers.config.ConfigPropertyKey
import `in`.shabinder.soundbound.providers.config.ConfigPropertyValue
import `in`.shabinder.soundbound.utils.GlobalJson
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.collections.List

/**
 * Impl class must override [defaultConfig] if [Config] is [ProviderConfiguration.Configuration]
 *  as it will provide correct handling with [ProviderConfiguration.key] .
 *  */
interface ConfigHandler : Dependencies {
  val prefKey: String

  fun getPreferenceKey(key: ConfigPropertyKey): String =
    "${this@ConfigHandler.prefKey}.${key.key}"

  val isConfigAvailable: Boolean get() = false
  suspend fun getConfigKeys(): List<ConfigPropertyKey> = emptyList()
  suspend fun saveConfigValues(configValues: List<ConfigPropertyValue>) {
    configValues.forEach {
      when (it) {
        is ConfigPropertyValue.Single -> saveValue(it.key, it.value)
        is ConfigPropertyValue.List -> saveValue(it.key, it.value)
      }
    }
  }

  suspend fun saveConfigValues(configValue: ConfigPropertyValue) {
    saveConfigValues(listOf(configValue))
  }
}

suspend fun ConfigHandler.getConfigValues(): List<ConfigPropertyValue> {
  return getConfigKeys().map { key: ConfigPropertyKey ->
    when (key) {
      is ConfigPropertyKey.Single -> {
        ConfigPropertyValue.Single(
          key = key,
          value = getSavedValueOrDefault(key)
        )
      }

      is ConfigPropertyKey.List -> {
        ConfigPropertyValue.List(
          key = key,
          value = getSavedValueOrDefault(key)
        )
      }
    }
  }
}

fun List<ConfigPropertyValue>.getValueForKey(key: ConfigPropertyKey.Single): JsonPrimitive {
  return first { it.key == key }.let {
    (it as ConfigPropertyValue.Single).value
  }
}

fun List<ConfigPropertyValue>.getValueForKey(key: ConfigPropertyKey.List): JsonArray {
  return first { it.key == key }.let {
    (it as ConfigPropertyValue.List).value
  }
}

fun ConfigHandler.getSavedValueOrDefault(
  key: ConfigPropertyKey.Single,
  defaultValue: JsonPrimitive = key.defaultValue
): JsonPrimitive {
  return getSavedValue(key) ?: defaultValue
}

fun ConfigHandler.getSavedValueOrDefault(
  key: ConfigPropertyKey.List,
  defaultValue: JsonArray = key.defaultValue
): JsonArray {
  return getSavedValue(key) ?: defaultValue
}

fun ConfigHandler.getSavedValue(key: ConfigPropertyKey.List): JsonArray? {
  return runCatching {
    devicePreferences.getStringOrNull(getPreferenceKey(key))?.let {
      GlobalJson.parseToJsonElement(it) as JsonArray
    }
  }.getOrElse {
    it.printStackTrace()
    null
  }
}

fun ConfigHandler.getSavedValue(key: ConfigPropertyKey.Single): JsonPrimitive? {
  return runCatching {
    devicePreferences.getStringOrNull(getPreferenceKey(key))?.let {
      GlobalJson.parseToJsonElement(it) as JsonPrimitive
    }
  }.getOrElse {
    it.printStackTrace()
    null
  }
}

fun ConfigHandler.saveValue(key: ConfigPropertyKey.Single, value: JsonPrimitive) {
  devicePreferences.putString(getPreferenceKey(key), GlobalJson.encodeToString(value))
}

fun ConfigHandler.saveValue(key: ConfigPropertyKey.List, value: JsonArray) {
  devicePreferences.putString(getPreferenceKey(key), GlobalJson.encodeToString(value))
}
