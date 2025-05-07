package `in`.shabinder.soundbound.utils

import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MappedFlowProperty<T, R>(
    private val delegate: FlowReadWriteProperty<T>,
    private val toMapped: (T) -> R,
    private val fromMapped: (R) -> T,
) : FlowReadWriteProperty<R> {

  override val flow: Flow<R> by lazy {
    delegate.flow.map(toMapped)
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

fun <T, R> FlowReadWriteProperty<T>.map(
  toMapped: (T) -> R,
  fromMapped: (R) -> T,
): FlowReadWriteProperty<R> {
  return MappedFlowProperty(
    delegate = this,
    toMapped = toMapped,
    fromMapped = fromMapped,
  )
}

fun <T> FlowReadWriteProperty<T>.map(
  mapper: (T) -> T,
): FlowReadWriteProperty<T> {
  return MappedFlowProperty(
    delegate = this,
    toMapped = mapper,
    fromMapped = mapper,
  )
}
