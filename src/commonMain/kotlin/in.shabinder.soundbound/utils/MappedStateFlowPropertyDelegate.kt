package `in`.shabinder.soundbound.utils

import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.StateFlow

private class MappedStateFlowPropertyDelegate<T, R>(
  private val delegate: StateFlowReadWriteProperty<T>,
  private val toMapped: (T) -> R,
  private val fromMapped: (R) -> T,
) : StateFlowReadWriteProperty<R> {

  override val flow: StateFlow<R> by lazy {
    delegate.flow.mapSync(toMapped)
  }

  override var value: R
    get() = toMapped(delegate.value)
    set(value) {
      delegate.value = fromMapped(value)
    }

  override fun getValue(thisRef: Any?, property: KProperty<*>): R = value

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
    this.value = value
  }
}

fun <T, R> StateFlowReadWriteProperty<T>.map(
  toMapped: (T) -> R,
  fromMapped: (R) -> T,
): StateFlowReadWriteProperty<R> {
  return MappedStateFlowPropertyDelegate(
    delegate = this,
    toMapped = toMapped,
    fromMapped = fromMapped,
  )
}

fun <T> StateFlowReadWriteProperty<T>.map(
  mapper: (T) -> T,
): StateFlowReadWriteProperty<T> {
  return MappedStateFlowPropertyDelegate(
    delegate = this,
    toMapped = mapper,
    fromMapped = mapper,
  )
}

