package `in`.shabinder.soundbound.utils

import androidx.compose.runtime.Stable
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

@Stable
class StateFlowPropertyDelegate<T>(
  private val key: String,
  private val defaultValue: T,
  private val devicePreferences: DevicePreferences,
  serializerProvider: () -> KSerializer<T>,
  private val onChange: ((old: T, new: T) -> Unit)? = null,
) : StateFlowReadWriteProperty<T> {
  // Resolve the serializer lazily. The no-arg reified serializer() default runs a reflective
  // kotlinx-serialization lookup (kotlin-reflect / kotlinx-metadata) whose class-name string
  // interning shows up as main-thread "Lock contention on InternTable" during cold start, because
  // all ~40 PreferenceManager props construct at once and each resolved its serializer eagerly
  // (Perfetto-proven on S23 Ultra: ~289ms). Deferring to first prop access means startup only pays
  // for the handful of props actually read then; the ~30 never-touched-at-startup props stay off
  // the critical path entirely.
  private val serializer: KSerializer<T> by lazy(serializerProvider)

  private val _flow by lazy { MutableStateFlow(readValue()) }
  override val flow: StateFlow<T> get() = _flow

  override var value: T
    get() = _flow.value
    set(value) {
      val current = _flow.value
      if (current == value) return

      /* Notify onChangeCallback */
      onChange?.invoke(current, value)

      // Update the value
      _flow.value = value
      if (value == null) {
        devicePreferences.remove(key)
      } else {
        devicePreferences.putSerializedString(key, value, serializer)
      }
    }

  override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
  }

  fun resetToDefault() {
    value = defaultValue
  }

  private fun readValue(): T {
    return devicePreferences.getSerializedOrNull(key, serializer) ?: defaultValue
  }
}

// serializerProvider is a lambda, not a resolved KSerializer, so the reflective serializer()
// default is not invoked at call time (PreferenceManager init) - only when the delegate first
// touches its value. See StateFlowPropertyDelegate for the cold-start rationale.
inline fun <reified T> propWithFlow(
  key: String,
  defaultValue: T,
  devicePreferences: DevicePreferences,
  noinline serializerProvider: () -> KSerializer<T> = { serializer() },
  noinline onChange: ((old: T, new: T) -> Unit)? = null,
): StateFlowReadWriteProperty<T> = StateFlowPropertyDelegate(
  key = key,
  defaultValue = defaultValue,
  devicePreferences = devicePreferences,
  serializerProvider = serializerProvider,
  onChange = onChange,
)

// have this as a `StateFlowReadWriteProperty`,
// https://github.com/Kotlin/kotlinx.coroutines/issues/2631#issuecomment-870565860
inline fun <reified T, reified S> propWithFlow(
  key: String,
  defaultValue: T,
  devicePreferences: DevicePreferences,
  noinline serializerProvider: () -> KSerializer<S> = { serializer() },
  noinline onChange: ((old: T, new: T) -> Unit)? = null,
  crossinline saveAs: (T) -> S,
  crossinline loadFrom: (S) -> T,
): StateFlowReadWriteProperty<T> = StateFlowPropertyDelegate(
  key = key,
  defaultValue = saveAs(defaultValue),
  devicePreferences = devicePreferences,
  serializerProvider = serializerProvider,
  onChange = { old, new -> onChange?.invoke(loadFrom(old), loadFrom(new)) },
).map(
  toMapped = { loadFrom(it) },
  fromMapped = { saveAs(it) },
)

inline fun <reified T> DevicePreferences.propWithFlow(
  key: String,
  defaultValue: T,
  noinline serializerProvider: () -> KSerializer<T> = { serializer() },
  noinline onChange: ((old: T, new: T) -> Unit)? = null,
): StateFlowReadWriteProperty<T> = propWithFlow(
  key = key,
  defaultValue = defaultValue,
  devicePreferences = this,
  serializerProvider = serializerProvider,
  onChange = onChange,
)

inline fun <reified T, reified S> DevicePreferences.propWithFlow(
  key: String,
  defaultValue: T,
  noinline serializerProvider: () -> KSerializer<S> = { serializer() },
  noinline onChange: ((old: T, new: T) -> Unit)? = null,
  crossinline saveAs: (T) -> S,
  crossinline loadFrom: (S) -> T,
): StateFlowReadWriteProperty<T> = propWithFlow(
  key = key,
  defaultValue = defaultValue,
  devicePreferences = this,
  serializerProvider = serializerProvider,
  onChange = onChange,
  saveAs = saveAs,
  loadFrom = loadFrom,
)
