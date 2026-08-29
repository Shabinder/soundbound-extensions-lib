package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

/**
 * A real browser engine, exposed to any Zipline extension as a general-purpose capability.
 *
 * ## Why this exists
 *
 * Extensions run in QuickJS, which is deliberately small and embedded. That is fine for ordinary
 * work, but some tasks require a full browser engine:
 *
 * - it lacks modern built-ins (`String.prototype.at`, `Object.hasOwn`, `BigInt`, class static
 *   blocks, the RegExp `d` flag), so many published libraries will not even load;
 * - parsing and evaluating large third-party programs can be substantially slower;
 * - it has no DOM or genuine browser environment for pages that require one.
 *
 * So the app hands extensions the engine, and the extension keeps the logic. Anything an extension
 * needs a browser for — attestation, signature deciphering, a login flow, scraping a page that
 * only renders under JavaScript, calling an API that demands browser-shaped headers and cookies —
 * ships in the **bundle** and reaches users over the air.
 *
 * ## The rule this interface exists to enforce
 *
 * **Nothing here may name a provider, site, or protocol.** Provider-specific behavior belongs in
 * its over-the-air bundle. If a future capability cannot be expressed here without naming a
 * provider, the fix is a more general primitive, not a special case.
 *
 * ## Shape
 *
 * Session-scoped: [newSession] returns a handle whose pages, globals and cookies are isolated from
 * other sessions, so an extension can hold an expensive prepared context (a parsed player, an
 * initialised virtual machine, a logged-in cookie jar) and make many cheap calls against it.
 *
 * Every call returns a [BrowserResult] rather than throwing, because an exception crossing the
 * Zipline boundary loses its type and stack. Callers must treat failure as ordinary: engines are
 * absent on some platforms, and pages fail.
 *
 * Page-to-host communication is **pull-based** ([drainMessages], [pollEvents]) rather than
 * callback-based. Callbacks over Zipline are possible but fragile; draining a queue is simple,
 * survives a slow consumer, and maps onto what every engine already offers (wry's
 * `drainIpcMessages`, Android's `addJavascriptInterface`, iOS's `WKScriptMessageHandler`).
 */
interface HeadlessBrowser : ZiplineService {

  /**
   * What this platform's engine can actually do. Engines differ, and a platform may have no
   * engine at all, so an extension should branch on this rather than assume. Cheap; safe to call
   * before every use.
   *
   * Report what is *actually* true, and fix the engine when something is missing. A capability
   * flag is for a real platform limit, never a shortcut around a bug in code we control.
   */
  suspend fun capabilities(): BrowserCapabilities

  /**
   * Opens an isolated session and returns its handle, or an error when no engine is available.
   * Sessions are a scarce resource: some engines cannot be recreated within a process, so
   * implementations may reuse one under the hood. Always pair with [closeSession].
   */
  suspend fun newSession(config: BrowserSessionConfig = BrowserSessionConfig()): BrowserResult

  /** Releases a session's state. Safe to call twice; unknown handles are ignored. */
  suspend fun closeSession(sessionId: String)

  /**
   * Loads a URL, or a supplied document when [NavigateRequest.html] is set. Returns once the
   * engine reports the load finished, or on timeout.
   */
  suspend fun navigate(sessionId: String, request: NavigateRequest): BrowserResult

  /**
   * Evaluates JavaScript in the session's current page and returns its completion value as a
   * string.
   *
   * For asynchronous work, set [EvaluateRequest.awaitExpression]: the engine evaluates
   * [EvaluateRequest.script], then polls that expression until it yields a value that is neither
   * `null` nor `undefined`. A completion value alone cannot express "wait for this", and most
   * interesting browser work is asynchronous.
   */
  suspend fun evaluate(sessionId: String, request: EvaluateRequest): BrowserResult

  /**
   * Blocks until a condition holds: a JavaScript expression becomes truthy, the current URL
   * matches a pattern, or the page finishes loading. Returns what it observed.
   */
  suspend fun waitFor(sessionId: String, request: WaitRequest): BrowserResult

  /** The session's current URL. */
  suspend fun currentUrl(sessionId: String): BrowserResult

  /** The session's current document title. */
  suspend fun title(sessionId: String): BrowserResult

  /** The current document's serialised HTML. */
  suspend fun content(sessionId: String): BrowserResult

  /** History and load control, where the engine supports it. */
  suspend fun navigateBack(sessionId: String): BrowserResult

  suspend fun navigateForward(sessionId: String): BrowserResult

  suspend fun reload(sessionId: String): BrowserResult

  suspend fun stopLoading(sessionId: String): BrowserResult

  /**
   * Cookies visible to the session, optionally narrowed to [url]. This is what makes login flows
   * expressible in a bundle: navigate, let the user authenticate, read the jar.
   */
  suspend fun getCookies(sessionId: String, url: String? = null): BrowserCookiesResult

  suspend fun setCookie(sessionId: String, cookie: BrowserCookie): BrowserResult

  /** Clears cookies for [url], or all of them when null. */
  suspend fun clearCookies(sessionId: String, url: String? = null): BrowserResult

  /**
   * Takes and clears everything the page has posted to the host since the last drain.
   *
   * The page posts with the function named by [BrowserCapabilities.messageChannelName], e.g.
   * `soundboundBridge.post("...")`. This is the extension's inbound hook: a script can watch the
   * DOM, intercept `fetch`, or signal completion, and the extension collects it without any
   * callback plumbing.
   */
  suspend fun drainMessages(sessionId: String): BrowserMessagesResult

  /**
   * Takes and clears engine-level events observed since the last poll — navigations started and
   * finished, load failures, console output.
   *
   * Navigation events are how an extension implements a redirect hook (the usual shape of an
   * OAuth flow): navigate, poll until a URL matching the expected redirect appears, read its
   * query.
   */
  suspend fun pollEvents(sessionId: String): BrowserEventsResult
}

/**
 * @param hasEngine False when the platform has no browser engine wired up. Everything else is
 *   meaningless when this is false.
 * @param canSeedDocument Whether [NavigateRequest.html] works. True on all three platforms today.
 *   It is still reported rather than assumed: this was briefly false on desktop because our own
 *   webview fork cancelled every navigation when no listener was registered, and the honest fix
 *   was repairing the fork rather than writing the limitation into this contract.
 * @param canSetBaseUrl Whether a seeded document can be given an origin.
 * @param canInterceptNavigation Whether navigation events are reported to [HeadlessBrowser.pollEvents].
 * @param canReadCookies / canWriteCookies Cookie jar access.
 * @param canOverrideUserAgent Whether [BrowserSessionConfig.userAgent] is honoured.
 * @param canRunAtDocumentStart Whether [BrowserSessionConfig.initScript] runs before page scripts.
 *   When false it is applied after load, which is too late to patch globals a page reads eagerly.
 * @param messageChannelName Global the page uses to post to the host, or null when unsupported.
 * @param engineName Identifies the backing engine for diagnostics, e.g. "android-webview",
 *   "wkwebview", "wry".
 */
@Serializable
data class BrowserCapabilities(
  val hasEngine: Boolean = false,
  val canSeedDocument: Boolean = false,
  val canSetBaseUrl: Boolean = false,
  val canInterceptNavigation: Boolean = false,
  val canReadCookies: Boolean = false,
  val canWriteCookies: Boolean = false,
  val canOverrideUserAgent: Boolean = false,
  val canRunAtDocumentStart: Boolean = false,
  val messageChannelName: String? = null,
  val engineName: String = "none",
)

/**
 * @param initScript Evaluated once per page, ideally before the page's own scripts — the place to
 *   install polyfills, stub globals, or hook `fetch`/`XMLHttpRequest`.
 * @param userAgent Overrides the engine default where supported.
 * @param blockNetwork Denies the engine network access entirely. Useful when the host does all the
 *   HTTP itself and the page is only a compute environment; it also means no remote page can be
 *   served in place of the intended one.
 * @param incognito Requests a non-persistent profile where supported.
 * @param label Human-readable tag for diagnostics.
 */
@Serializable
data class BrowserSessionConfig(
  val initScript: String? = null,
  val userAgent: String? = null,
  val blockNetwork: Boolean = false,
  val incognito: Boolean = true,
  val label: String = "extension",
)

/**
 * @param url URL to load. Ignored when [html] is set.
 * @param html Document to load directly instead of fetching a URL.
 * @param baseUrl Origin for [html], where supported.
 * @param headers Extra request headers, where supported.
 * @param waitForLoad Whether to return only once loading finishes.
 * @param timeoutMs Upper bound for the whole call.
 */
@Serializable
data class NavigateRequest(
  val url: String? = null,
  val html: String? = null,
  val baseUrl: String? = null,
  val headers: Map<String, String> = emptyMap(),
  val waitForLoad: Boolean = true,
  val timeoutMs: Long = 30_000L,
)

/**
 * @param script JavaScript to evaluate; its completion value is the result unless
 *   [awaitExpression] is set.
 * @param awaitExpression Polled after [script] runs until it yields a non-null, non-undefined
 *   value, which becomes the result. This is how asynchronous work is awaited.
 * @param pollIntervalMs How often to re-check [awaitExpression].
 * @param timeoutMs Upper bound for the whole call, polling included.
 */
@Serializable
data class EvaluateRequest(
  val script: String,
  val awaitExpression: String? = null,
  val pollIntervalMs: Long = 100L,
  val timeoutMs: Long = 30_000L,
)

/**
 * Exactly one of [expression], [urlPattern] or [loadFinished] should be set.
 *
 * @param expression JavaScript polled until truthy.
 * @param urlPattern Substring or regular expression the current URL must match.
 * @param urlPatternIsRegex Whether [urlPattern] is a regular expression rather than a substring.
 * @param loadFinished Wait until the page stops loading.
 */
@Serializable
data class WaitRequest(
  val expression: String? = null,
  val urlPattern: String? = null,
  val urlPatternIsRegex: Boolean = false,
  val loadFinished: Boolean = false,
  val pollIntervalMs: Long = 100L,
  val timeoutMs: Long = 30_000L,
)

/**
 * @param value Result of the call, or null when it produced nothing or failed.
 * @param error Human-readable reason when the call failed. Null error with null value means the
 *   call succeeded and produced no value, which is not an error.
 */
@Serializable
data class BrowserResult(
  val value: String? = null,
  val error: String? = null,
) {
  val isSuccess: Boolean get() = error == null

  companion object {
    fun ok(value: String? = null) = BrowserResult(value = value)

    fun failure(reason: String) = BrowserResult(error = reason)
  }
}

@Serializable
data class BrowserCookie(
  val name: String,
  val value: String,
  val domain: String? = null,
  val path: String? = null,
  val secure: Boolean = false,
  val httpOnly: Boolean = false,
  /** Seconds since the Unix epoch, or null for a session cookie. */
  val expiresAtSeconds: Long? = null,
)

@Serializable
data class BrowserCookiesResult(
  val cookies: List<BrowserCookie> = emptyList(),
  val error: String? = null,
)

@Serializable
data class BrowserMessagesResult(
  val messages: List<String> = emptyList(),
  val error: String? = null,
)

/**
 * @param type What happened. See [BrowserEventType].
 * @param url URL involved, where one applies.
 * @param detail Extra text: a console message, a failure reason.
 * @param timestampMs Host clock when observed, for ordering across a drain.
 */
@Serializable
data class BrowserEvent(
  val type: BrowserEventType,
  val url: String? = null,
  val detail: String? = null,
  val timestampMs: Long = 0L,
)

@Serializable
enum class BrowserEventType {
  NAVIGATION_STARTED,
  NAVIGATION_FINISHED,
  NAVIGATION_FAILED,
  CONSOLE_MESSAGE,
  PAGE_CRASHED,
}

@Serializable
data class BrowserEventsResult(
  val events: List<BrowserEvent> = emptyList(),
  val error: String? = null,
)
