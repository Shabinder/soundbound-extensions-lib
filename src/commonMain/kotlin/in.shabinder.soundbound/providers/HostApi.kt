package `in`.shabinder.soundbound.providers

/**
 * The host API level this lib defines: an integer that goes up whenever a lib release adds or changes a host
 * service an extension can call. An extension built against this lib declares it as `min_host_api` in its repository
 * listing when it needs a service that older hosts lack; a host whose [LEVEL] is lower skips that extension and
 * reports it as needing a newer app instead of failing at call time.
 *
 * Levels: 42 = lib 0.42 (first level; source-neutral names, LyricsText, granular HostPermission). Hosts before it
 * do not read `min_host_api` and treat every listing as compatible.
 */
object HostApi {
    const val LEVEL: Int = 42
}
