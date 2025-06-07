package `in`.shabinder.soundbound.concurrency

import `in`.shabinder.soundbound.utils.safeRunCatching
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


/**
 * De-dupe multiple calls for a resource. A new [SingleFlightGroup] should be created
 * for each async call that requires debouncing & de-duping.
 */
class SingleFlightGroup<T> {
  private val mu = Mutex()
  private var inFlight: CompletableDeferred<Result<T>>? = null
  private var waitCount = 0

  suspend fun await(): Result<T>? {
    mu.lock()
    val job = inFlight
    mu.unlock()

    return job?.await()
  }

  val isActive: Boolean
    get() = mu.isLocked || (inFlight != null)

  suspend fun singleFlight(message: String? = null, isForced: Boolean = false, context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): T {
    return singleFlightResult(isForced, context, block)
      .getOrThrow()
  }

  suspend fun singleFlightOrNull(message: String? = null, isForced: Boolean = false, context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): T? {
    return singleFlightResult(isForced, context, block)
      .getOrNull()
  }

  suspend fun singleFlightResult(isForced: Boolean = false, context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): Result<T> {
    return makeFlight(isForced, context, block)
  }

  /**
   * Executes [block] on the first invocation if there are no in-flight calls already made waiting
   * for the response. If a call is already in-flight then waits for the response without kicking off
   * a new call to [block]
   * @param block the function to execute
   * @return the response produced or throws an exception
   */
  private suspend fun makeFlight(isForced: Boolean = false, context: CoroutineContext, block: suspend () -> T): Result<T> = withContext(context) {
    try {
      mu.lock()

      // if we are forced to make a new call, cancel the in-flight call
      if (isForced) {
        inFlight?.cancel()
        inFlight = null
      }

      val job = inFlight

      // we already have an ongoing job, join that.
      if (job != null) {
        waitCount++
        mu.unlock()

        // wait for value to be produced
        job.join()

        mu.withLock {
          waitCount--
          if (waitCount == 0) {
            // last one out - clear the current job
            inFlight = null
          }
        }
        return@withContext job.await()
      }

      // no active jobs, create a new in-flight request
      val deferred = CompletableDeferred<Result<T>>()
      inFlight = deferred
      mu.unlock()
      safeRunCatching { block() }.let { deferred.complete(it) }
      val result = deferred.await()
      result
    } finally {
      // Check if we are the sole owner of the in-flight job, and if so, clear it
      mu.withLock {
        // If no one is waiting for the result, clear the in-flight job
        if (waitCount == 0) {
          inFlight = null
        }
      }
    }
  }
}
