package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * How the host turns fetched bytes into playable bytes: an ordered pipeline of [ProcessOp]s that the host executes
 * natively (JCA on JVM/Android, CommonCrypto on iOS, pure-Kotlin fallbacks), streaming and seekable where the ops allow.
 * It covers stripe-encrypted CDNs, counter-mode files, per-chunk IVs, compressed wrappers and vendor headers as data, so
 * a new format needs an extension update, not an app release. For anything ops cannot express, [ProcessOp.ExtensionTransform]
 * hands chunks back to the extension (slow, but fully open).
 *
 * Ops run in order on the byte stream. Framing ops ([ProcessOp.Chunked], [ProcessOp.Stripe]) set how the NEXT cipher sees
 * the stream: as fixed-size chunks (each its own IV/counter scope) or as every n-th chunk. Without framing a cipher sees
 * the stream as one message. [ProcessOp.Skip] / [ProcessOp.Trim] / [ProcessOp.Range] cut the output.
 *
 * Feature-detect with `PlaybackHost.playbackCapabilities().processing`. The host rejects a pipeline it cannot run
 * (unknown op, algorithm, mode, over [ProcessingLimits]) with a clean playback/download failure; it never plays or saves
 * unprocessed bytes.
 */
@Immutable
@Serializable
data class ProcessingSpec(val ops: List<ProcessOp>) {
  companion object {
    /** AES-128-CTR over the whole stream, counter starting at [ivHex]. */
    fun aesCtr(keyHex: String, ivHex: String = ZERO16) = ProcessingSpec(
      listOf(ProcessOp.Cipher(CipherAlg.AES, CipherMode.CTR, keyHex, IvSpec.Fixed(ivHex))),
    )

    /** AES-128-CBC over the whole stream; [pkcs7] trims the padding. */
    fun aesCbc(keyHex: String, ivHex: String = ZERO16, pkcs7: Boolean = false) = ProcessingSpec(
      listOf(ProcessOp.Cipher(CipherAlg.AES, CipherMode.CBC, keyHex, IvSpec.Fixed(ivHex), if (pkcs7) Padding.PKCS7 else Padding.NONE)),
    )

    /** Every [every]-th [chunkSize]-byte chunk Blowfish-CBC encrypted, each with the same IV (a common stripe format). */
    fun blowfishStripe(keyHex: String, ivHex: String = "0001020304050607", chunkSize: Int = 2048, every: Int = 3) = ProcessingSpec(
      listOf(
        ProcessOp.Stripe(chunkSize, every),
        ProcessOp.Cipher(CipherAlg.BLOWFISH, CipherMode.CBC, keyHex, IvSpec.PerChunk(ivHex)),
      ),
    )

    private const val ZERO16 = "00000000000000000000000000000000"
  }
}

@Serializable
sealed interface ProcessOp {
  /** Frame the stream into [size]-byte chunks for the next cipher (a short last chunk passes through untouched). */
  @Serializable
  data class Chunked(val size: Int) : ProcessOp

  /** Like [Chunked], but only chunks with `index % every == phase` are processed; the rest pass through. */
  @Serializable
  data class Stripe(val chunkSize: Int, val every: Int, val phase: Int = 0) : ProcessOp

  /** Drop [prefix] bytes from the start of the output (a vendor header in front of the container). */
  @Serializable
  data class Skip(val prefix: Long) : ProcessOp

  /** Drop [tail] bytes from the end of the output. */
  @Serializable
  data class Trim(val tail: Long) : ProcessOp

  /** Keep output bytes [from, to) only; [to] null = to the end. */
  @Serializable
  data class Range(val from: Long, val to: Long? = null) : ProcessOp

  /**
   * Decrypt with [alg] in [mode]. [keyHex] is the key in hex; [iv] says where each IV/counter comes from.
   * [CipherMode.STREAM] is for stream ciphers ([CipherAlg.RC4], [CipherAlg.CHACHA20], [CipherAlg.XOR]); [CipherMode.GCM_DECRYPT]
   * expects the 16-byte tag at the end of each message (or chunk) and fails on a bad tag.
   */
  @Serializable
  data class Cipher(
    val alg: CipherAlg,
    val mode: CipherMode,
    val keyHex: String,
    val iv: IvSpec = IvSpec.None,
    val padding: Padding = Padding.NONE,
  ) : ProcessOp

  /** Decompress. Sequential: the pipeline stops being seekable. */
  @Serializable
  data class Inflate(val format: InflateFormat) : ProcessOp

  /**
   * Fallback for anything ops cannot express: the host calls the extension's `ExtensionPlaybackSource.transform(id,
   * chunkIndex, bytes)` for each [chunkSize]-byte chunk (a short last chunk too), off the main thread, with read-ahead.
   * Slow (a JS round trip per chunk); prefer native ops when capabilities list them.
   */
  @Serializable
  data class ExtensionTransform(val chunkSize: Int, val id: String) : ProcessOp
}

@Serializable
enum class CipherAlg { AES, BLOWFISH, DES, DES3, CHACHA20, RC4, XOR }

@Serializable
enum class CipherMode { ECB, CBC, CTR, CFB, OFB, GCM_DECRYPT, STREAM }

@Serializable
enum class Padding { NONE, PKCS7 }

@Serializable
enum class InflateFormat { GZIP, ZLIB, RAW }

@Serializable
enum class Endian { BIG, LITTLE }

@Serializable
sealed interface IvSpec {
  /** No IV (ECB, RC4, XOR). */
  @Serializable
  data object None : IvSpec

  /** One IV / initial counter for the whole message. */
  @Serializable
  data class Fixed(val hex: String) : IvSpec

  /** The same IV restarts every chunk (needs [ProcessOp.Chunked] or [ProcessOp.Stripe] before the cipher). */
  @Serializable
  data class PerChunk(val hex: String) : IvSpec

  /** Each chunk's IV is [baseHex] plus the chunk index, as a [endian] integer over the whole IV. */
  @Serializable
  data class CounterFromChunk(val baseHex: String, val endian: Endian = Endian.BIG) : IvSpec

  /** CBC across chunks: each chunk's IV is the previous chunk's last ciphertext block; the first uses [firstHex]. */
  @Serializable
  data class ChainedPreviousBlock(val firstHex: String) : IvSpec
}

/** What the host will run; reported in `PlaybackCapabilities.processing`. */
@Serializable
data class ProcessingCapabilities(
  val ops: Set<String> = emptySet(),
  val algs: Set<CipherAlg> = emptySet(),
  val modes: Set<CipherMode> = emptySet(),
  val ivSpecs: Set<String> = emptySet(),
  val inflate: Set<InflateFormat> = emptySet(),
  val limits: ProcessingLimits = ProcessingLimits(),
  /** [ProcessOp.ExtensionTransform] is supported, and costs a guest round trip per chunk. */
  val extensionTransformSlow: Boolean = true,
)

@Serializable
data class ProcessingLimits(
  val maxOps: Int = 0,
  val maxChunkBytes: Int = 0,
  /** Bytes the host buffers for one pipeline (windows + read-ahead). */
  val memoryBudgetBytes: Long = 0,
)
