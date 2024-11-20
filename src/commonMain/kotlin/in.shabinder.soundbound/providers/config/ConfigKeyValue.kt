package `in`.shabinder.soundbound.providers.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.collections.List as KotlinList

@Serializable
sealed class ConfigPropertyKey<T> {
  abstract val key: String
  abstract val defaultValue: T
  abstract val isRequired: Boolean
  abstract val isUserConfigurable: Boolean
  abstract val serializer: KSerializer<T>

  @Serializable
  data class Primitive<T> constructor(
    override val key: String,
    override val defaultValue: T,
    override val serializer: KSerializer<T>,
    override val isRequired: Boolean = false,
    override val isUserConfigurable: Boolean = false
  ) : ConfigPropertyKey<T>() {
    companion object {
      inline fun <reified T> create(
        key: String,
        defaultValue: T,
        isRequired: Boolean = false,
        isUserConfigurable: Boolean = false
      ): Primitive<T> {
        return Primitive(
          key = key,
          defaultValue = defaultValue,
          serializer = serializer<T>(),
          isRequired = isRequired,
          isUserConfigurable = isUserConfigurable
        )
      }
    }
  }

  @Serializable
  data class List<T>(
    override val key: String,
    override val defaultValue: KotlinList<T>,
    val isMultiSelection: Boolean = false,
    override val isRequired: Boolean = false,
    override val serializer: KSerializer<KotlinList<T>>,
    override val isUserConfigurable: Boolean = false
  ) : ConfigPropertyKey<KotlinList<T>>() {
    companion object {
      inline fun <reified T> create(
        key: String,
        defaultValue: KotlinList<T>,
        isMultiSelection: Boolean = false,
        isRequired: Boolean = false,
        isUserConfigurable: Boolean = false
      ): List<T> {
        return List(
          key = key,
          defaultValue = defaultValue,
          isMultiSelection = isMultiSelection,
          isRequired = isRequired,
          isUserConfigurable = isUserConfigurable,
          serializer = serializer<KotlinList<T>>()
        )
      }
    }
  }
}

@Serializable
sealed class ConfigPropertyValue<T>() {
  abstract val key: ConfigPropertyKey<T>

  @Serializable
  data class Primitive<T>(
    override val key: ConfigPropertyKey.Primitive<T>,
    val value: T
  ) : ConfigPropertyValue<T>()

  @Serializable
  data class List<T>(
    override val key: ConfigPropertyKey.List<T>,
    val value: KotlinList<T>
  ) : ConfigPropertyValue<KotlinList<T>>()
}

