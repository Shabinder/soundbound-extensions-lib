package `in`.shabinder.soundbound.matcher

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/*
 * Recording gates and two-tier name similarity, ported from songmirror `songmirror/engine/matching.py`
 * (https://github.com/ahnafnafee/songmirror at 3897c5f, MIT License, Copyright (c) 2026 Ahnaf An Nafee; full text
 * in this repository's NOTICE). Changes from the original: Kotlin; no anyascii romanisation (so Latin-vs-native-script
 * titles do not match yet); seconds instead of ms; YouTube-upload title cleanup and the store ±3 s / YouTube ±15 s
 * windows from SoundBound's Docs/RESEARCH/song-identity-benchmark; marker spellings canonicalised in the compared name
 * instead of a Jaro-Winkler floor; the duration score is spotDL's `100·e^(−0.1·|Δs|)` (spotDL, MIT).
 */

/** Bumped whenever accept/reject behaviour changes, so persisted automatic matches are re-checked. */
const val MATCHER_VERSION = 2

/** [score] is 0..100; [accepted] is whether the candidate is safe to treat as the same recording. */
data class MatchVerdict(val score: Float, val accepted: Boolean)

object RecordingMatch {
    const val ARTIST_THRESHOLD = 0.92
    /** Loose (token-set) title similarity only counts when the durations agree this closely. */
    const val LOOSE_NAME_WINDOW_SEC = 2.5
    /** Hard duration windows: store vs store, and when either side is a YouTube upload (intros/outros). */
    const val STORE_TOLERANCE_SEC = 3L
    const val YOUTUBE_TOLERANCE_SEC = 15L

    private val I = RegexOption.IGNORE_CASE

    private val PAREN_FEAT = Regex("""[(\[]\s*(?:feat|featuring|ft|with)\b.*?[)\]]""", I)
    private val TRAILING_FEAT = Regex("""\s+(?:feat|featuring|ft)\.?\s+.*?(?=\s*[(\[{]|\s+[-–—]\s+|$)""", I)
    private val FEAT_PREFIX = Regex("""^[\s(\[]*(?:feat|featuring|ft|with)\b\.?\s*""", I)
    private val GUEST_SPLIT = Regex("""\s*(?:,|&|\band\b)\s*""", I)

    // Title decoration that never changes the recording (benchmark §3 + songmirror catalog_name).
    private val CLEANUP = listOf(
        Regex("""\s+\|.*$"""), // "Arz Kiya Hai | Coke Studio Bharat"
        Regex("""(?:\s+[-–—|:]?\s*|\s*[(\[]\s*)coke studio\b.*$""", I), // Apple: "Arz Kiya Hai  Coke Studio Bharat"
        Regex("""[(\[]\s*from\b[^)\]]*[)\]]""", I), // (From "Zeher"), (From _Zeher_)
        Regex("""\s+[-–—]\s+from\s+["'“‘_].*$""", I), // - From "Movie"
        Regex("""@[\w.]+"""), // @adityarikhari
        Regex("""\bunplg'?d\b""", I), // UNPLG'd (a channel, not "unplugged")
        // Only brackets made of these words: "(Live Video)" keeps its live marker.
        Regex("""[(\[]\s*(?:(?:official|music|lyrics?|lyrical|full|hd|4k|song)\s+)*(?:video|audio|visuali[sz]er|lyrics?|lyrical|song|hd|4k)\s*[)\]]""", I),
        Regex("""[(\[]\s*(?:(?:\d{4}\s+)?re-?master(?:ed)?(?:\s+\d{4})?(?:\s+version)?|clean|explicit|bonus track)\s*[)\]]""", I),
        Regex("""\s*[-–—]\s*(?:(?:\d{4}\s+)?re-?master(?:ed)?(?:\s+\d{4})?(?:\s+version)?|clean|explicit|bonus track)\s*$""", I),
    )
    private val TRAILING_DASH = Regex("""[\s\-–—]+$""")

    /** Word-bounded, matched on the normalised (lowercase, punctuation → space) title. Order is songmirror's. */
    private val VERSION_PATTERNS: List<Pair<String, Regex>> = listOf(
        "live" to Regex("""\b(?:live|canli|ao vivo|en vivo)\b"""),
        "acoustic" to Regex("""\b(?:acoustic|akustik|ac[uú]stic[oa]|acoustique|unplugged|stripped)\b"""),
        "remix" to Regex("""\b(?:remix|rework|mashup)\b"""),
        "dub" to Regex("""\b(?:dub|dubbed)\b"""),
        "mix" to Regex("""\bmix\b"""),
        "edit" to Regex("""\bedit\b"""),
        "vip" to Regex("""\bvip\b"""),
        "instrumental" to Regex("""\binstrumental\b"""),
        "karaoke" to Regex("""\bkaraoke\b"""),
        "piano" to Regex("""\bpiano\b"""),
        "demo" to Regex("""\bdemo\b"""),
        "session" to Regex("""\bsession\b"""),
        "alternate-speed" to Regex("""\b(?:sped up|speed up|slowed(?: down)?|nightcore)\b"""),
        "mix-format" to Regex("""\b(?:mono|stereo)\b"""),
        "cover" to Regex("""\bcover\b"""),
        "a-cappella" to Regex("""\ba ?ca?pp?ella\b"""),
        "orchestral" to Regex("""\b(?:orchestral|orchestra|symphonic)\b"""),
        "re-recording" to Regex("""\bre ?record(?:ed|ing)\b"""),
        "reverb" to Regex("""\breverb\b"""),
        "spatial" to Regex("""\b(?:8d|binaural)\b"""),
        "lofi" to Regex("""\blo ?fi\b"""),
        "bass-boosted" to Regex("""\bbass ?boost(?:ed)?\b"""),
        "reprise" to Regex("""\breprise\b"""),
        "solo" to Regex("""\bsolo\b"""),
        "alternate" to Regex("""\b(?:alternate|alternative)\b"""),
        // From lib #70 (C-02): commentary uploads are never the recording.
        "reaction" to Regex("""\b(?:reaction|reacts?|reacting|review)\b"""),
    )
    private val VERSION_WORD = Regex("""\bversion\b""")
    private val VER_ABBREV = Regex("""\bver\b""")
    private val PART_SPLIT = Regex("""\s+[-–—]\s+|[()\[\]{}]""")

    private val TOPIC = Regex("""\s*-\s*topic\s*$""", I)
    private val CREDIT_SPLIT =
        Regex("""\u0000|\\0|\s*(?:,|;|&|\||\s+x\s+|\b(?:feat|ft)\.?\s|\bfeaturing\s|\band\b|\bwith\b)\s*""", I)
    private val PLACEHOLDER_ARTISTS = setOf("", "release", "release topic", "unknown", "unknown artist", "various artists")
    private val TRIBUTE = Regex("""\b(?:tribute|karaoke|covers?|renditions?)\b""")

    // From lib #70 (C-02): fold common Latin accents so "Beyoncé" == "Beyonce". No NFD in commonMain (JS/iOS).
    // ponytail: Latin table only; extend when a catalogue needs more. Indic vowel signs are untouched.
    private const val ACCENTED = "àáâãäåāăąçćčďèéêëēėęěìíîïīįñńňòóôõöøōőùúûüūůűųýÿžźżśšşřłğ"
    private const val PLAIN = "aaaaaaaaacccdeeeeeeeeiiiiiinnnoooooooouuuuuuuuyyzzzsssrlg"

    /** Unicode-aware: letters, digits and combining marks (Devanagari/Gurmukhi vowel signs) survive in any script. */
    fun normalizeText(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        val lower = value.lowercase().replace("i̇", "i")
        val sb = StringBuilder(lower.length)
        for (c in lower) {
            val keep = c.isLetterOrDigit() || c.category == CharCategory.NON_SPACING_MARK ||
                c.category == CharCategory.COMBINING_SPACING_MARK
            val folded = ACCENTED.indexOf(c).let { if (it < 0) c else PLAIN[it] }
            sb.append(if (keep) folded else ' ')
        }
        return sb.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun normalizeIsrc(value: String?): String = value.orEmpty().filter { it.isLetterOrDigit() }.uppercase()

    private fun withoutFeatures(name: String) = TRAILING_FEAT.replace(PAREN_FEAT.replace(name, " "), "")

    /** Strips decoration that names the same recording: feat. credits, (From "…"), | Coke Studio …, @handle, UNPLG'd, (Official Video), remaster/clean/explicit. */
    fun cleanTitle(title: String?): String {
        var s = withoutFeatures(title.orEmpty())
        while (true) {
            val before = s
            for (r in CLEANUP) s = r.replace(s, " ")
            s = TRAILING_DASH.replace(s, "")
            if (s == before) break
        }
        return s.trim()
    }

    /** The compared title: cleaned, normalised, version spellings canonicalised ("Akustik", "Acoustic Version" → "acoustic"). */
    fun looseName(title: String?): String {
        val raw = title.orEmpty()
        var n = VER_ABBREV.replace(normalizeText(cleanTitle(raw)), "version")
        for ((marker, pattern) in VERSION_PATTERNS) {
            n = Regex("""(?:${pattern.pattern})(?: version)?""").replace(n, marker)
        }
        return n.ifEmpty { normalizeText(raw).ifEmpty { raw.lowercase() } }
    }

    fun versionMarkers(title: String?): Set<String> {
        val n = VER_ABBREV.replace(normalizeText(cleanTitle(title)), "version")
        val markers = VERSION_PATTERNS.filter { it.second.containsMatchIn(n) }.map { it.first }.toMutableSet()
        if (markers.isEmpty() && VERSION_WORD.containsMatchIn(n)) markers += "alternate-version"
        return markers
    }

    /** The title with every version marker word removed ("Kesariya (Live)" -> "Kesariya"); used by the all-same-label fallback. */
    fun stripVersionWords(title: String?): String {
        var n = VER_ABBREV.replace(normalizeText(cleanTitle(title)), "version")
        for ((_, pattern) in VERSION_PATTERNS) n = pattern.replace(n, " ")
        n = VERSION_WORD.replace(n, " ")
        return n.split(' ').filter { it.isNotBlank() }.joinToString(" ").ifEmpty { title.orEmpty() }
    }

    /** Recording family plus named mix/venue/version details: "First DJ Remix" ≠ "Second DJ Remix". */
    fun versionSignature(title: String?): Pair<Set<String>, Set<String>> {
        val cleaned = cleanTitle(title)
        val markers = versionMarkers(cleaned)
        val details = mutableSetOf<String>()
        for (part in cleaned.split(PART_SPLIT).drop(1)) {
            if (versionMarkers(part).isEmpty()) continue
            var n = VER_ABBREV.replace(normalizeText(part), "version")
            for ((marker, pattern) in VERSION_PATTERNS) n = pattern.replace(n, marker)
            if (markers != setOf("alternate-version")) n = VERSION_WORD.replace(n, "")
            val detail = n.split(' ').filter { it.isNotBlank() }.sorted().joinToString(" ")
            if (detail != markers.sorted().joinToString(" ")) details += detail
        }
        return markers to details
    }

    fun featuredArtists(title: String?): List<String> = buildList {
        for (r in listOf(PAREN_FEAT, TRAILING_FEAT)) for (m in r.findAll(title.orEmpty())) {
            val credit = FEAT_PREFIX.replace(m.value, "").trimEnd(')', ']', ' ')
            addAll(credit.split(GUEST_SPLIT).map { it.trim() }.filter { it.isNotEmpty() })
        }
    }

    private fun hasCredit(guest: String, credits: String) = " ${normalizeText(credits)} ".contains(" ${normalizeText(guest)} ")

    /** Same version family and details, and every explicit guest is credited on the other side. */
    fun versionsCompatible(title: String, artists: List<String>, candTitle: String, candArtists: List<String>): Boolean {
        if (versionSignature(title) != versionSignature(candTitle)) return false
        val features = featuredArtists(title)
        val candFeatures = featuredArtists(candTitle)
        val credits = (artists + features).joinToString(" ")
        val candCredits = (candArtists + candFeatures).joinToString(" ")
        return features.all { hasCredit(it, candCredits) } && candFeatures.all { hasCredit(it, credits) }
    }

    /** Whole performer names: splits , ; & x feat with and \u0000, drops "- Topic" and placeholder credits. */
    fun splitCredits(artists: List<String>): List<String> =
        artists.flatMap { TOPIC.replace(it, "").split(CREDIT_SPLIT) }
            .map { it.trim() }
            .filter { normalizeText(it) !in PLACEHOLDER_ARTISTS }

    private fun nameVariants(name: String) = normalizeText(name).let { setOf(it, it.replace(" ", "")) }

    /** 0..1. A tribute/karaoke/cover credit only matches another; otherwise one side's credits must all appear on the other. */
    fun artistSimilarity(artists: List<String>, candArtists: List<String>): Double {
        val left = splitCredits(artists)
        val right = splitCredits(candArtists)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        fun tributes(names: List<String>) = TRIBUTE.findAll(normalizeText(names.joinToString(" "))).map { it.value }.toSet()
        if (tributes(left) != tributes(right)) return 0.0
        if ((nameVariants(left.joinToString(" ")) intersect nameVariants(right.joinToString(" "))).isNotEmpty()) return 1.0
        fun allCredits(l: List<String>, r: List<String>): Double {
            val rv = r.flatMap(::nameVariants)
            return l.minOf { name -> nameVariants(name).maxOf { a -> rv.maxOf { b -> Similarity.tokenSort(a, b) } } }
        }
        return max(allCredits(left, right), allCredits(right, left))
    }

    fun isYoutube(url: String?): Boolean = url != null && (url.contains("youtube.com") || url.contains("youtu.be"))

    /**
     * songmirror `score_candidate`: version + feature gate, artist gate, hard duration window, then accept when the
     * strict title similarity is high, or the loose one is high and the durations agree within [LOOSE_NAME_WINDOW_SEC].
     */
    fun score(
        title: String, artists: List<String>, durationSec: Long,
        candTitle: String, candArtists: List<String>, candDurationSec: Long,
        youtube: Boolean = false,
    ): MatchVerdict {
        val reject = MatchVerdict(0f, false)
        if (!versionsCompatible(title, artists, candTitle, candArtists)) return reject
        val artistSim = artistSimilarity(artists, candArtists)
        val durationScore: Double
        val durationClose: Boolean
        if (durationSec > 0 && candDurationSec > 0) {
            val delta = abs(durationSec - candDurationSec)
            if (delta > if (youtube) YOUTUBE_TOLERANCE_SEC else STORE_TOLERANCE_SEC) return reject
            durationScore = exp(-0.1 * delta)
            durationClose = delta <= LOOSE_NAME_WINDOW_SEC
        } else {
            durationScore = 0.5
            durationClose = false
        }
        val a = looseName(title)
        val b = looseName(candTitle)
        val strict = Similarity.tokenSort(a, b)
        val nameSim = if (durationClose) max(strict, Similarity.tokenSet(a, b)) else strict
        val score = 0.45 * nameSim + 0.35 * artistSim + 0.20 * durationScore
        val strong = durationClose && nameSim >= 0.78 && artistSim >= ARTIST_THRESHOLD
        val fuzzy = strict >= 0.88 && artistSim >= ARTIST_THRESHOLD
        return MatchVerdict((score * 100).toFloat(), strong || fuzzy)
    }
}

/** Indel-normalised similarity (rapidfuzz `fuzz.ratio` semantics) and its token variants, all 0..1. */
object Similarity {
    fun ratio(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        // LCS length, two rows. ponytail: O(n·m); titles and credits are short.
        var prev = IntArray(b.length + 1)
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                cur[j] = if (a[i - 1] == b[j - 1]) prev[j - 1] + 1 else max(prev[j], cur[j - 1])
            }
            val t = prev; prev = cur; cur = t
        }
        return 2.0 * prev[b.length] / (a.length + b.length)
    }

    private fun tokens(s: String) = s.split(' ').filter { it.isNotEmpty() }

    fun tokenSort(a: String, b: String): Double =
        if (a.isBlank() || b.isBlank()) 0.0 else ratio(tokens(a).sorted().joinToString(" "), tokens(b).sorted().joinToString(" "))

    /** 1.0 when one side's tokens are a subset of the other's (rapidfuzz `token_set_ratio`). */
    fun tokenSet(a: String, b: String): Double {
        val ta = tokens(a).toSet()
        val tb = tokens(b).toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val common = ta intersect tb
        val diffAb = ta - tb
        val diffBa = tb - ta
        if (common.isNotEmpty() && (diffAb.isEmpty() || diffBa.isEmpty())) return 1.0
        val t0 = common.sorted().joinToString(" ")
        val t1 = (t0 + " " + diffAb.sorted().joinToString(" ")).trim()
        val t2 = (t0 + " " + diffBa.sorted().joinToString(" ")).trim()
        return maxOf(if (t0.isEmpty()) 0.0 else ratio(t0, t1), if (t0.isEmpty()) 0.0 else ratio(t0, t2), ratio(t1, t2))
    }
}
