package `in`.shabinder.soundbound.zipline


/**
 * Stand-in for anywhere a real browser engine is absent - a platform without one, or a headless
 * test harness.
 *
 * It reports `hasEngine = false` and fails every call with a clear reason, rather than throwing or
 * pretending to work. Extensions are expected to check
 * [HeadlessBrowser.capabilities] first and degrade — the same contract as a platform that
 * genuinely has no engine — so this is honest rather than a silent hole.
 */
object UnavailableHeadlessBrowser : HeadlessBrowser {

  private const val REASON = "no browser engine is available on this platform"

  override suspend fun capabilities(): BrowserCapabilities = BrowserCapabilities(hasEngine = false)

  override suspend fun newSession(config: BrowserSessionConfig): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun closeSession(sessionId: String) = Unit

  override suspend fun navigate(sessionId: String, request: NavigateRequest): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun evaluate(sessionId: String, request: EvaluateRequest): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun waitFor(sessionId: String, request: WaitRequest): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun currentUrl(sessionId: String): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun title(sessionId: String): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun content(sessionId: String): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun navigateBack(sessionId: String): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun navigateForward(sessionId: String): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun reload(sessionId: String): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun stopLoading(sessionId: String): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun getCookies(sessionId: String, url: String?): BrowserCookiesResult = BrowserCookiesResult(error = REASON)

  override suspend fun setCookie(sessionId: String, cookie: BrowserCookie): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun clearCookies(sessionId: String, url: String?): BrowserResult = BrowserResult.failure(REASON)

  override suspend fun drainMessages(sessionId: String): BrowserMessagesResult = BrowserMessagesResult(error = REASON)

  override suspend fun pollEvents(sessionId: String): BrowserEventsResult = BrowserEventsResult(error = REASON)
}
