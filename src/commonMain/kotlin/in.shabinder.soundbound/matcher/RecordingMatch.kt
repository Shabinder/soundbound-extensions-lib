package `in`.shabinder.soundbound.matcher

import `in`.shabinder.soundbound.text.Romanizer
import kotlin.math.abs
import kotlin.math.max

/*
 * Recording gates and two-tier name similarity, ported from songmirror `songmirror/engine/matching.py`
 * (https://github.com/ahnafnafee/songmirror at 3897c5f, MIT License, Copyright (c) 2026 Ahnaf An Nafee; full text
 * in this repository's NOTICE). Changes from the original: Kotlin; the shared text.Romanizer (SimpMusic-derived, GPL-3.0)
 * instead of anyascii; seconds instead of ms; YouTube-upload title cleanup and the store ±3 s / YouTube ±15 s windows from
 * SoundBound's Docs/RESEARCH/song-identity-benchmark; marker spellings canonicalised in the compared name instead of a
 * Jaro-Winkler floor; a banded duration score instead of the linear term.
 * Also from SoulSync `core/matching_engine.py` (https://github.com/Nezreka/SoulSync at 32480b35a, MIT License,
 * Copyright (c) 2025 SoulSync; NOTICE): the derivative-artist keywords (salute/parody/soundalike/backing track) and the
 * extended / rmx / rough cut version keywords.
 */

/** Bumped whenever accept/reject behaviour changes, so persisted automatic matches are re-checked. */
const val MATCHER_VERSION = 4

/** How much a verdict can be trusted, weakest first. Callers gate account writes on it (e.g. only [STRONG] or [EXACT]). */
enum class MatchConfidence { NONE, LOOSE, STRONG, EXACT }

/** Why a candidate was accepted or rejected; [confidence] follows from it. */
enum class MatchReason(val confidence: MatchConfidence) {
    /** Equal ISRC and no metadata contradicts it. */
    ISRC(MatchConfidence.EXACT),
    /** Strict title similarity, artists and a duration within [RecordingMatch.LOOSE_NAME_WINDOW_SEC] all agree. */
    STRICT_TITLE_AND_DURATION(MatchConfidence.STRONG),
    /** Only the loose (token-subset) title tier passed, carried by close durations. */
    LOOSE_TITLE_AND_DURATION(MatchConfidence.LOOSE),
    /** Strict title and artists agree; the duration is unknown or only inside the wide window. */
    STRICT_TITLE(MatchConfidence.LOOSE),
    /** Every candidate carried the same version label the query lacks; best of those (see `orderResults`). */
    SAME_LABEL_FALLBACK(MatchConfidence.LOOSE),
    VERSION_MISMATCH(MatchConfidence.NONE),
    FEATURED_GUEST_MISMATCH(MatchConfidence.NONE),
    ARTIST_MISMATCH(MatchConfidence.NONE),
    DURATION_MISMATCH(MatchConfidence.NONE),
    TITLE_MISMATCH(MatchConfidence.NONE),
}

/** [score] is 0..100; [accepted] is whether the candidate is safe to treat as the same recording. */
data class MatchVerdict(
    val score: Float,
    val accepted: Boolean,
    val reason: MatchReason = if (accepted) MatchReason.STRICT_TITLE else MatchReason.TITLE_MISMATCH,
) {
    val confidence: MatchConfidence get() = reason.confidence
}

object RecordingMatch {
    const val ARTIST_THRESHOLD = 0.92
    /** Loose (token-set) title similarity only counts when the durations agree this closely. */
    const val LOOSE_NAME_WINDOW_SEC = 2.5
    /** Hard duration windows: store vs store, and when either side is a YouTube upload (intros/outros). */
    const val STORE_TOLERANCE_SEC = 3L
    const val YOUTUBE_TOLERANCE_SEC = 15L
    /** An equal ISRC still loses to a store-vs-store length gap above this (songmirror CATALOG_DURATION_TOLERANCE_MS). */
    const val ID_TOLERANCE_SEC = 5L
    /** A [soundKey] shorter than this (one or two consonants) is too common to identify a name. */
    const val MIN_SOUND_KEY = 3
    /** Strict-tier title similarity (songmirror). */
    const val STRICT_NAME = 0.88

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
        "live" to Regex("""\b(?:live|canli|ao vivo|en vivo|raivu|raibu|raibeu)\b"""), // ライヴ ライブ 라이브
        "acoustic" to Regex("""\b(?:acoustic|akustik|ac[uú]stic[oa]|acoustique|unplugged|stripped)\b"""),
        "remix" to Regex("""\b(?:remix|rmx|rework|mashup)\b"""),
        "dub" to Regex("""\b(?:dub|dubbed)\b"""),
        "mix" to Regex("""\bmix\b"""),
        "edit" to Regex("""\bedit\b"""),
        "extended" to Regex("""\bextended\b"""), // SoulSync
        "restrung" to Regex("""\brestrung\b"""), // string re-arrangement ("Faded (Restrung)")
        "vip" to Regex("""\bvip\b"""),
        "instrumental" to Regex("""\binst(?:rumental)?\b"""),
        "karaoke" to Regex("""\bkaraoke\b"""),
        "piano" to Regex("""\bpiano\b"""),
        "demo" to Regex("""\b(?:demo|rough cut)\b"""),
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

    private val NUMBER = Regex("""\d+""")
    private val TOPIC = Regex("""\s*-\s*topic\s*$""", I)
    private val CREDIT_SPLIT =
        Regex("""\u0000|\\0|\s*(?:,|;|&|\||\s+x\s+|\b(?:feat|ft)\.?\s|\bfeaturing\s|\band\b|\bwith\b)\s*""", I)
    private val PLACEHOLDER_ARTISTS = setOf("", "release", "release topic", "unknown", "unknown artist", "various artists")
    // songmirror's tribute markers plus SoulSync's derivative-artist keywords.
    private val TRIBUTE = Regex("""\b(?:tribute|karaoke|covers?|renditions?|salute|parody|soundalike|backing (?:track|band))\b""")

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

    /**
     * Latin reading of a normalised string in any script the shared [Romanizer] covers (Камин → kamin, तुम ही हो → tum
     * hi ho, 사랑해 → saranghae, 月亮 → yue liang); Latin text comes back unchanged.
     */
    fun romanize(normalized: String): String =
        if (normalized.all { it.code < 0x370 }) normalized else normalizeText(Romanizer.latin(normalized, spaceHan = false))

    private fun nonLatin(s: String) = s.any { it.code >= 0x370 && it.isLetter() }

    /**
     * Coarse sound key for comparing two Latin readings of one name made by different spelling systems: no spaces, no
     * vowels, voicing and aspiration merged, doubles collapsed (ரவுடி "ravuti" and "Rowdy" → "rwt"; معسل "msl" and
     * "Masaal" → "msl"; Ukrainian read as Russian "zagrali" and "zahraly" → "skrl"). Only used when a name holds
     * non-Latin letters, so Latin-vs-Latin scoring is untouched.
     * ponytail: one key for every script; per-language keys (Thai tones, Arabic dialect q/g) if this over-merges.
     */
    fun soundKey(latin: String): String {
        var s = latin.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
        for ((from, to) in SOUND_DIGRAPHS) s = s.replace(from, to)
        val sb = StringBuilder(s.length)
        for (c in s) {
            val m = SOUND_MERGE[c] ?: c
            if (m == '_' || sb.lastOrNull() == m) continue
            sb.append(m)
        }
        return sb.toString()
    }

    private val SOUND_DIGRAPHS = listOf("ng" to "n", "ph" to "f", "sh" to "s", "ch" to "c", "zh" to "j", "ts" to "s", "ks" to "x", "h" to "")
    private val SOUND_MERGE = mapOf(
        'a' to '_', 'e' to '_', 'i' to '_', 'o' to '_', 'u' to '_', 'y' to '_', 'w' to 'v', 'b' to 'p', 'd' to 't',
        'g' to 'k', 'q' to 'k', 'c' to 'c', 'z' to 's', 'x' to 'k',
    )

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
        val n = VER_ABBREV.replace(romanize(normalizeText(cleanTitle(title))), "version")
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
            var n = VER_ABBREV.replace(romanize(normalizeText(part)), "version")
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

    private fun hasCredit(guest: String, credits: String) =
        " ${romanize(normalizeText(credits))} ".contains(" ${romanize(normalizeText(guest))} ")

    /** Same version family and details, and every explicit guest is credited on the other side. */
    fun versionsCompatible(title: String, artists: List<String>, candTitle: String, candArtists: List<String>): Boolean =
        versionConflict(title, artists, candTitle, candArtists) == null

    private fun versionConflict(title: String, artists: List<String>, candTitle: String, candArtists: List<String>): MatchReason? {
        if (versionSignature(title) != versionSignature(candTitle)) return MatchReason.VERSION_MISMATCH
        val features = featuredArtists(title)
        val candFeatures = featuredArtists(candTitle)
        val credits = (artists + features).joinToString(" ")
        val candCredits = (candArtists + candFeatures).joinToString(" ")
        val ok = features.all { hasCredit(it, candCredits) } && candFeatures.all { hasCredit(it, credits) }
        return if (ok) null else MatchReason.FEATURED_GUEST_MISMATCH
    }

    /** Whole performer names: splits , ; & x feat with and \u0000, drops "- Topic" and placeholder credits. */
    fun splitCredits(artists: List<String>): List<String> =
        artists.flatMap { TOPIC.replace(it, "").split(CREDIT_SPLIT) }
            .map { it.trim() }
            .filter { normalizeText(it) !in PLACEHOLDER_ARTISTS }

    private fun nameVariants(name: String) = normalizeText(name).let { n ->
        val r = romanize(n)
        setOf(n, r).flatMap { listOf(it, it.replace(" ", "")) }.toSet() + if (nonLatin(n)) setOf(soundKey(r)) else emptySet()
    }

    /** 0..1. A tribute/karaoke/cover credit only matches another; otherwise one side's credits must all appear on the other. */
    fun artistSimilarity(artists: List<String>, candArtists: List<String>): Double {
        val left = splitCredits(artists)
        val right = splitCredits(candArtists)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        fun tributes(names: List<String>) = TRIBUTE.findAll(romanize(normalizeText(names.joinToString(" ")))).map { it.value }.toSet()
        if (tributes(left) != tributes(right)) return 0.0
        if ((nameVariants(left.joinToString(" ")) intersect nameVariants(right.joinToString(" "))).isNotEmpty()) return 1.0
        val cross = nonLatin(left.joinToString(" ")) || nonLatin(right.joinToString(" "))
        fun variants(name: String) = nameVariants(name) + if (cross) setOf(soundKey(romanize(normalizeText(name)))) else emptySet()
        if (cross && soundKey(romanize(normalizeText(left.joinToString(" ")))).let { k -> k.length >= MIN_SOUND_KEY && k == soundKey(romanize(normalizeText(right.joinToString(" ")))) }) return 1.0
        fun allCredits(l: List<String>, r: List<String>): Double {
            val rv = r.flatMap(::variants)
            return l.minOf { name -> variants(name).maxOf { a -> rv.maxOf { b -> Similarity.tokenSort(a, b) } } }
        }
        return max(allCredits(left, right), allCredits(right, left))
    }

    fun isYoutube(url: String?): Boolean = url != null && (url.contains("youtube.com") || url.contains("youtu.be"))

    /**
     * Banded duration agreement, 0..1: 1 within 1 s, 0.9 within 2.5 s, then 0.7 / 0.5 / 0.3 / 0.15 up to 5 / 8 / 12 / 15 s.
     * Within a band every candidate ties, so a few seconds of encoder padding never outranks a better title.
     */
    fun durationBand(deltaSec: Long): Double = when (abs(deltaSec)) {
        in 0L..1L -> 1.0
        2L -> 0.9 // ≤ LOOSE_NAME_WINDOW_SEC in whole seconds
        in 3L..5L -> 0.7
        in 6L..8L -> 0.5
        in 9L..12L -> 0.3
        in 13L..15L -> 0.15
        else -> 0.0
    }

    /**
     * songmirror `recording_metadata_compatible`: a hard identifier (equal ISRC) names the recording unless known
     * metadata contradicts it — another version/guest, another performer, or a length outside [ID_TOLERANCE_SEC]
     * ([YOUTUBE_TOLERANCE_SEC] when either side is YouTube). Missing metadata never disproves the identifier.
     * Null = compatible, else why not.
     */
    fun idConflict(
        title: String, artists: List<String>, durationSec: Long,
        candTitle: String, candArtists: List<String>, candDurationSec: Long,
        youtube: Boolean = false,
    ): MatchReason? {
        versionConflict(title, artists, candTitle, candArtists)?.let { return it }
        if (splitCredits(artists).isNotEmpty() && splitCredits(candArtists).isNotEmpty() &&
            artistSimilarity(artists, candArtists) < ARTIST_THRESHOLD
        ) return MatchReason.ARTIST_MISMATCH
        if (durationSec > 0 && candDurationSec > 0 &&
            abs(durationSec - candDurationSec) > if (youtube) YOUTUBE_TOLERANCE_SEC else ID_TOLERANCE_SEC
        ) return MatchReason.DURATION_MISMATCH
        return null
    }

    /**
     * songmirror `score_candidate`: version + feature gate, artist gate, hard duration window, then accept when the
     * strict title similarity is high, or the loose one is high and the durations agree within [LOOSE_NAME_WINDOW_SEC].
     * Titles compare over raw and romanised variants (Камин == Kamin).
     */
    fun score(
        title: String, artists: List<String>, durationSec: Long,
        candTitle: String, candArtists: List<String>, candDurationSec: Long,
        youtube: Boolean = false,
    ): MatchVerdict {
        versionConflict(title, artists, candTitle, candArtists)?.let { return MatchVerdict(0f, false, it) }
        // "Part 1" vs "Part 2": one token apart scores 0.89, above the strict tier, so numbers must agree when both
        // titles carry some.
        val numbers = NUMBER.findAll(looseName(title)).map { it.value.trimStart('0') }.toSet()
        val candNumbers = NUMBER.findAll(looseName(candTitle)).map { it.value.trimStart('0') }.toSet()
        if (numbers.isNotEmpty() && candNumbers.isNotEmpty() && numbers != candNumbers) {
            return MatchVerdict(0f, false, MatchReason.TITLE_MISMATCH)
        }
        val artistSim = artistSimilarity(artists, candArtists)
        val durationScore: Double
        val durationClose: Boolean
        if (durationSec > 0 && candDurationSec > 0) {
            val delta = abs(durationSec - candDurationSec)
            if (delta > if (youtube) YOUTUBE_TOLERANCE_SEC else STORE_TOLERANCE_SEC) {
                return MatchVerdict(0f, false, MatchReason.DURATION_MISMATCH)
            }
            durationScore = durationBand(delta)
            durationClose = delta <= LOOSE_NAME_WINDOW_SEC
        } else {
            durationScore = 0.5
            durationClose = false
        }
        // Compared without version words: the gate above made them agree, and a long shared "instrumental" would
        // otherwise carry two different titles over the threshold ("SHE (inst)" vs "Deutgosipeo (inst.)").
        val bare = stripVersionWords(title).let(::normalizeText)
        val candBare = stripVersionWords(candTitle).let(::normalizeText)
        val a = setOf(bare, romanize(bare))
        val b = setOf(candBare, romanize(candBare))
        fun best(sim: (String, String) -> Double) = a.maxOf { x -> b.maxOf { y -> sim(x, y) } }
        // Across scripts, also compare unspaced readings (Thai/kana write no spaces) and sound keys, both long enough
        // that a short key cannot collide by chance; that tier only ever counts as strict.
        val crossSim = if (!nonLatin(title) && !nonLatin(candTitle)) 0.0 else {
            val ra = romanize(bare)
            val rb = romanize(candBare)
            val ka = soundKey(ra)
            val kb = soundKey(rb)
            maxOf(
                Similarity.ratio(ra.replace(" ", ""), rb.replace(" ", "")),
                if (minOf(ka.length, kb.length) >= MIN_SOUND_KEY) Similarity.ratio(ka, kb) else 0.0,
            )
        }
        val strict = max(best(Similarity::tokenSort), if (crossSim >= STRICT_NAME) crossSim else 0.0)
        val loose = if (durationClose) best(Similarity::tokenSet) else 0.0
        val nameSim = max(strict, loose)
        val score = ((0.45 * nameSim + 0.35 * artistSim + 0.20 * durationScore) * 100).toFloat()
        val artistOk = artistSim >= ARTIST_THRESHOLD
        val reason = when {
            !artistOk -> MatchReason.ARTIST_MISMATCH
            durationClose && strict >= STRICT_NAME -> MatchReason.STRICT_TITLE_AND_DURATION
            durationClose && nameSim >= 0.78 -> MatchReason.LOOSE_TITLE_AND_DURATION
            strict >= STRICT_NAME -> MatchReason.STRICT_TITLE
            else -> MatchReason.TITLE_MISMATCH
        }
        return MatchVerdict(score, reason.confidence != MatchConfidence.NONE, reason)
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
