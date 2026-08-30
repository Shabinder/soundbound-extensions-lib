package `in`.shabinder.soundbound.zipline

import androidx.compose.runtime.Immutable
import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

/** Description of one immutable runtime asset carried by the host application. */
@Immutable
@Serializable
data class RuntimeAssetDescriptor(
  val id: String,
  val version: String,
  val contentType: String,
  val contentEncoding: String,
  val encodedSizeBytes: Long,
  val decodedSizeBytes: Long,
  val decodedSha256: String,
)

/**
 * Lazily supplies large, versioned runtime assets that ship with the app-side extension loader.
 *
 * The contract is deliberately provider-neutral. Extensions own asset identifiers, evaluation,
 * compatibility and fallback policy; the app is only an inert byte registry. Returning null means
 * that this app version does not carry the requested asset.
 */
interface RuntimeAssets : ZiplineService {
  /** Enumerates every asset this host can provide, including exact compatibility versions. */
  suspend fun catalog(): List<RuntimeAssetDescriptor>

  /** Returns encoded bytes only for an exact id/version match. */
  suspend fun read(id: String, version: String): ByteArray?
}

/** Used by direct extension targets and old hosts that do not bundle runtime assets. */
object UnavailableRuntimeAssets : RuntimeAssets {
  override suspend fun catalog(): List<RuntimeAssetDescriptor> = emptyList()
  override suspend fun read(id: String, version: String): ByteArray? = null
}
