package `in`.shabinder.soundbound.matcher

/**
 * Lyrics text rules shared by the host and lyrics extensions, so a lyric judged one version or one time by an extension
 * is judged the same by the app.
 */
object LyricsText {
    /**
     * Title words that name a separate edition of a song: a lyric for one must never answer for another. Word-bounded,
     * case-insensitive. Includes language editions (catalogues list "Despacito (Mandarin Version)").
     */
    val VERSION_TAGS: List<String> = listOf(
        "live", "remix", "acoustic", "instrumental", "karaoke", "cover", "slowed", "sped up",
        "reverb", "lofi", "lo-fi", "unplugged", "mashup", "8d", "nightcore", "reprise",
        "mandarin", "cantonese", "chinese", "japanese", "korean", "english version", "spanish version",
        "hindi version",
    )

    private val TAG_PATTERNS = VERSION_TAGS.map { it to Regex("""\b${Regex.escape(it)}\b""") }

    fun versionTags(title: String): Set<String> {
        val lower = title.lowercase()
        return TAG_PATTERNS.filter { it.second.containsMatchIn(lower) }.mapTo(LinkedHashSet()) { it.first }
    }

    /** The titles name different editions ([versionTags] differ). */
    fun versionConflict(queryTitle: String, candidateTitle: String): Boolean =
        versionTags(queryTitle) != versionTags(candidateTitle)

    /**
     * LRC line time tag: `[m:ss]`, `[mm:ss]`, `[mm:ss.x..xxx]`, `[mm:ss:xx]` (colon fraction seen in some sources).
     * Groups: minutes, seconds, fraction.
     */
    val LRC_TIME_TAG: Regex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    /** Enhanced-LRC word stamp `<mm:ss.xx>`, same groups as [LRC_TIME_TAG]. */
    val LRC_WORD_TAG: Regex = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>""")

    /** Milliseconds of a [LRC_TIME_TAG] / [LRC_WORD_TAG] match. One fraction digit is tenths, two hundredths, three ms. */
    fun lrcMillis(match: MatchResult): Long {
        val (min, sec, frac) = match.destructured
        val millis = when (frac.length) {
            0 -> 0L
            1 -> frac.toLong() * 100
            2 -> frac.toLong() * 10
            else -> frac.toLong()
        }
        return min.toLong() * 60_000 + sec.toLong() * 1000 + millis
    }

    /** Latest line time tag in [lyrics], ms; null when unsynced. */
    fun lastLrcMillis(lyrics: String): Long? = LRC_TIME_TAG.findAll(lyrics).maxOfOrNull(::lrcMillis)
}
