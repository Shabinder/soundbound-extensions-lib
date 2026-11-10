package `in`.shabinder.soundbound.providers.source

import `in`.shabinder.soundbound.models.PlatformQueryResult
import `in`.shabinder.soundbound.models.SourceModel
import `in`.shabinder.soundbound.providers.auth.AuthHandler.AuthMethod
import `in`.shabinder.soundbound.providers.library.LibraryCollections
import `in`.shabinder.soundbound.providers.library.LibraryTrackRef
import `in`.shabinder.soundbound.providers.library.ServerLibrary
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceDescriptorTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun minimalJsonDecodesToDefaults() {
    // What a host reading an older/smaller descriptor sees: only the required field present.
    val d = json.decodeFromString(SourceDescriptor.serializer(), """{"displayName":"X"}""")
    assertEquals(SourceDescriptor("X"), d)
    assertEquals("X", d.shortName)
    assertEquals(SyncDefaults(defaultTarget = false, privatePlaylistsSupported = false, likesPublic = false), d.syncDefaults)
    assertEquals(SearchDefaults(ungated = false, enabledByDefault = false, tileRank = 100, chipless = false), d.search)
    assertEquals(IdentitySpec(linkServiceInputRank = null, catalogStore = false, durationSlackSec = 3), d.identity)
    assertNull(d.library); assertNull(d.login); assertNull(d.instanceLabel)
    assertEquals("X", d.label)
  }

  @Test
  fun linkServiceInputRankKeepsItsWireKey() {
    // Bundles and hosts built against 0.40 exchange this key; the Kotlin rename must not change it.
    val wire = json.encodeToString(IdentitySpec.serializer(), IdentitySpec(linkServiceInputRank = 2))
    assertEquals(true, "\"odesliInputRank\":2" in wire, wire)
    assertEquals(2, json.decodeFromString(IdentitySpec.serializer(), """{"odesliInputRank":2}""").linkServiceInputRank)
  }

  @Test
  fun fullDescriptorRoundTrips() {
    val d = SourceDescriptor(
      displayName = "Server", shortName = "S", iconUrl = "https://soundbound.app/assets/ic_x.svg", brandColor = 0xFF112233,
      capabilities = SourceCapability.entries.toSet(),
      library = LibrarySyncSpec(writable = setOf(LibraryCollections.FAVOURITE_SONGS), exactFingerprints = true,
        keying = LibrarySyncSpec.Keying.ISRC, readOnlyPlaylists = listOf("^.*list=(LM|SE|RD.*)$")),
      login = LoginKind.DEVICE_CODE, syncDefaults = SyncDefaults(true, true, true),
      identity = IdentitySpec(1, true, 15, LinkRule("a(b)", "$1")),
      search = SearchDefaults(true, true, 0, true), isServer = true, instanceLabel = "Home",
      linkPrefixes = listOf("x://"),
    )
    assertEquals(d, json.decodeFromString(SourceDescriptor.serializer(), json.encodeToString(SourceDescriptor.serializer(), d)))
    assertEquals("Home", d.label)
  }

  @Test
  fun fallbackUsesSourceNameTailAndOnlyDeclaresSpecsForCapabilities() {
    val d = fallbackDescriptor("in.shabinder.soundbound.extensions.Navidrome#2", "icon", setOf(SourceCapability.LIBRARY_SYNC), LoginKind.COOKIE)
    assertEquals("Navidrome", d.displayName)
    assertEquals(LibrarySyncSpec(), d.library)
    assertNull(d.login, "login only with ACCOUNT_LOGIN")
    assertNull(fallbackDescriptor("a.B", null, emptySet()).library)
  }

  @Test
  fun loginKindFromAuthMethod() {
    val cookie = AuthMethod.CookieAuthAvailable("https://x", emptyList())
    assertEquals(LoginKind.COOKIE, loginKindOf(cookie, deviceLogin = false))
    assertEquals(LoginKind.WEBVIEW_JS, loginKindOf(cookie.copy(localStorageKeys = listOf(AuthMethod.CookieAuthAvailable.LocalStorageKey("t", "1"))), false))
    assertEquals(LoginKind.DEVICE_CODE, loginKindOf(AuthMethod.AuthNotNeeded, deviceLogin = true))
    assertNull(loginKindOf(AuthMethod.AuthNotNeeded, deviceLogin = false))
  }

  @Test
  fun matchForLibraryDefaultKeepsOwnLinksOnly() = runTest {
    val lib = object : ServerLibrary {
      override fun isLinkSupported(URL: String) = URL.startsWith("mine://")
      override suspend fun fetchPage(link: String, offset: Int, limit: Int) =
        PlatformQueryResult("", "", link, emptyList(), SourceModel("fake", "", true))
    }
    val refs = listOf("mine://song/1", "other://song/2").map { LibraryTrackRef(it, "t", listOf("a"), 100) }
    assertEquals(listOf("mine://song/1", null), lib.matchForLibrary(refs))
    assertEquals(listOf<Boolean?>(null, null), lib.verifyExists(refs.map { it.trackUrl }), "no check by default: unknown")
  }
}
