package `in`.shabinder.soundbound.utils

import kotlin.properties.ReadWriteProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FlowReadWriteProperty<T> : ReadWriteProperty<Any?, T> {
  val flow: Flow<T>
  var value: T
}

interface StateFlowReadWriteProperty<T> : FlowReadWriteProperty<T> {
  override val flow: StateFlow<T>
  override var value: T
}
