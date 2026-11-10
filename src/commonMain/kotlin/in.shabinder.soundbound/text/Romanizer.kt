/*
 * Portions adapted from SimpMusic core (GPL-3.0), org.simpmusic.lyrics.romanization
 * (LyricsRomanizer, RomanizationLanguage, IndicRomanizer, HangulRomanizer, KanaRomanizer,
 * CyrillicRomanizer) at maxrave-dev/core@c8999216813587995d941a3215dcf365e60e8422,
 * https://github.com/maxrave-dev/core (see NOTICE). Changes: Indic tables switched from Hunterian to
 * ISO 15919 letters and extended to every ISCII-aligned Brahmic block, word-final schwa deletion,
 * Gurmukhi addak/carriers, Hangul liquidisation, Cyrillic language guessed without a per-language
 * setting; Greek, Thai, Arabic/Persian/Urdu, Hebrew and Han (Unihan pinyin) added for [latin].
 */
package `in`.shabinder.soundbound.text

/**
 * Latin readings of non-Latin text, one implementation for lyrics (the app) and matching (the lib matcher).
 *
 * [romanize] is the display reading of one lyric line (ISO 15919 / Revised Romanization / Hepburn / BGN-PCGN
 * letters with their diacritics). [latin] is the ASCII reading of any mixed-script string, for comparing names
 * across stores ("तुम ही हो" → "tum hi ho", "月亮代表我的心" → "yue liang dai biao wo de xin").
 *
 * Limits (ponytail): kanji have no reading (needs a ~MB dictionary such as IPADIC), so Japanese Han stays as is;
 * Han → pinyin is per character with the most common reading (polyphones can be wrong); Arabic/Hebrew are
 * abjads, so short vowels are only read when pointed; Thai follows RTGS rules without a lexicon; Indic drops only
 * word-final schwa (medial deletion needs a lexicon).
 */
object Romanizer {

  /** The romanized line, or null when there is nothing useful to show under the original. */
  fun romanize(line: String): String? {
    if (line.isBlank()) return null
    val out = when {
      line.any { it.isKana() } -> if (line.any { it.isHan() }) null else Kana.romanize(line)
      line.any { it.isHangul() } -> Hangul.romanize(line)
      line.any { it in '\u0900'..'\u097F' } -> DEVANAGARI.romanize(line)
      line.any { it in '\u0A00'..'\u0A7F' } -> GURMUKHI.romanize(line)
      line.any { it in '\u0400'..'\u04FF' } -> Cyrillic.romanize(line)
      else -> INDIC.firstOrNull { s -> line.any { it in s.range } }?.romanize(line)
        ?: if (line.any { it.isGreek() }) Greek.romanize(line) else if (line.any { it in THAI }) Thai.romanize(line) else null
    } ?: return null
    return out.takeIf { it.isNotBlank() && it != line }
  }

  /**
   * ASCII Latin reading of [text], script by script; Latin and unknown characters pass through. Case is kept where
   * the script has it. Han becomes pinyin, one syllable per character, space-separated unless [spaceHan] is false
   * (then each Han run is one word, as names compare best); text that also holds kana is Japanese and keeps its Han.
   */
  fun latin(text: String, spaceHan: Boolean = true): String {
    if (text.all { it.code < 0x250 }) return text
    val japanese = text.any { it.isKana() }
    val sb = StringBuilder(text.length + 8)
    var i = 0
    while (i < text.length) {
      val engine = engineFor(text[i], japanese)
      if (engine == null) {
        sb.append(text[i]); i++; continue
      }
      var end = i + 1
      // A run continues over its own letters and marks; spaces end it (the engines handle words themselves).
      while (end < text.length && (engineFor(text[end], japanese) == engine || isMark(text[end]))) end++
      val run = text.substring(i, end)
      val out = if (engine === hanEngine && !spaceHan) run.mapNotNull { pinyin(it) }.joinToString("") else engine(run)
      if (engine === hanEngine && sb.isNotEmpty() && sb.last() != ' ') sb.append(' ')
      sb.append(out)
      if (engine === hanEngine && end < text.length && text[end] != ' ') sb.append(' ')
      i = end
    }
    return fold(sb)
  }

  // ---------------------------------------------------------------- dispatch

  private fun Char.isKana() = this in '\u3040'..'\u30FF'
  private fun Char.isHan() = this in '\u4E00'..'\u9FFF' || this in '\u3400'..'\u4DBF'
  private fun Char.isHangul() = this in '\uAC00'..'\uD7A3' || this in '\u1100'..'\u11FF'
  private fun Char.isGreek() = this in '\u0370'..'\u03FF' || this in '\u1F00'..'\u1FFF'
  private val THAI = '\u0E00'..'\u0E7F'
  private val ARABIC = '\u0600'..'\u06FF'
  private val HEBREW = '\u0590'..'\u05FF'
  private fun isMark(c: Char) = c.category == CharCategory.NON_SPACING_MARK || c.category == CharCategory.COMBINING_SPACING_MARK

  private val kanaEngine: (String) -> String = { Kana.romanize(it) }
  private val hangulEngine: (String) -> String = { Hangul.romanize(it) }
  private val cyrillicEngine: (String) -> String = { Cyrillic.romanize(it) }
  private val greekEngine: (String) -> String = { Greek.romanize(it) }
  private val thaiEngine: (String) -> String = { Thai.romanize(it) }
  private val arabicEngine: (String) -> String = { Abjad.arabic(it) }
  private val hebrewEngine: (String) -> String = { Abjad.hebrew(it) }
  private val hanEngine: (String) -> String = { run -> run.mapNotNull { pinyin(it) }.joinToString(" ") }
  private val indicEngines: List<Pair<CharRange, (String) -> String>> by lazy {
    (listOf(DEVANAGARI, GURMUKHI) + INDIC).map { s -> s.range to { run: String -> s.romanize(run, practical = true) } }
  }

  private fun engineFor(c: Char, japanese: Boolean): ((String) -> String)? = when {
    c.code < 0x370 -> null
    c.isKana() || (c.isHan() && japanese) -> if (c.isKana()) kanaEngine else null
    c.isHan() -> hanEngine
    c.isHangul() -> hangulEngine
    c in '\u0400'..'\u04FF' -> cyrillicEngine
    c.isGreek() -> greekEngine
    c in THAI -> thaiEngine
    c in ARABIC -> arabicEngine
    c in HEBREW -> hebrewEngine
    c in '\u0900'..'\u0DFF' -> indicEngines.firstOrNull { c in it.first }?.second
    else -> null
  }

  /** Most common Mandarin reading of a Han character, toneless ("愛" and "爱" → "ai"); null outside U+4E00..U+9FFF. */
  fun pinyin(c: Char): String? {
    val i = c.code - 0x4E00
    if (i !in 0 until 0x5200) return null
    val d = PinyinData.DIGITS
    val t = PinyinData.TABLE
    val n = d.indexOf(t[2 * i]) * d.length + d.indexOf(t[2 * i + 1])
    return if (n == 0) null else SYLLABLES[n - 1]
  }

  private val SYLLABLES by lazy { PinyinData.SYLLABLES.split(' ') }

  // Diacritics this file emits, folded to ASCII for [latin].
  private val FOLD = mapOf(
    'ā' to "a", 'ī' to "i", 'ū' to "u", 'ē' to "e", 'ō' to "o", 'ê' to "e", 'ô' to "o", 'ṭ' to "t", 'ḍ' to "d",
    'ṇ' to "n", 'ṅ' to "n", 'ñ' to "n", 'ś' to "sh", 'ṣ' to "sh", 'ṛ' to "r", 'ḷ' to "l", 'ḻ' to "zh", 'ṟ' to "r",
    'ṉ' to "n", 'ḥ' to "h", 'ġ' to "g", 'ẏ' to "y", 'ŭ' to "u", 'ö' to "o", 'ü' to "u", 'đ' to "dj", 'ć' to "c",
    'č' to "ch", 'ž' to "zh", 'š' to "sh", 'ṁ' to "n",
  )

  private fun fold(s: CharSequence): String = buildString(s.length) {
    var i = 0
    while (i < s.length) {
      val c = s[i]
      val lower = c.lowercaseChar()
      when {
        lower == 'r' && s.getOrNull(i + 1) == '\u0325' -> append(if (c.isUpperCase()) "Ri" else "ri").also { i++ }
        lower == 'ṁ' -> { // anusvara: homorganic nasal (सिंह → singh, कंबल → kambal)
          val next = s.getOrNull(i + 1)?.lowercaseChar()
          append(if (next == 'h') "ng" else if (next == 'p' || next == 'b' || next == 'm') "m" else "n")
        }
        lower == 'm' && s.getOrNull(i + 1) == '\u0310' -> append("n").also { i++ } // candrabindu
        isMark(c) -> Unit
        else -> {
          val f = FOLD[lower]
          if (f == null) append(c) else append(if (c.isUpperCase()) f.replaceFirstChar { it.uppercaseChar() } else f)
        }
      }
      i++
    }
  }

  // ---------------------------------------------------------------- Indic (abugida state machine)

  /**
   * A consonant carries an inherent vowel; a matra replaces it, a virama deletes it. Hindi, Punjabi, Gujarati and
   * Bengali also drop the inherent vowel at the end of a multi-syllable word (दिल → dil, not dila), except after a
   * conjunct (मित्र → mitra). Dravidian scripts and Odia keep it.
   *
   * ponytail: word-final schwa deletion only; medial deletion (धड़कन → dhaṛkan) needs a lexicon.
   */
  private class Indic(
    val range: CharRange,
    val consonants: Map<Char, String>,
    val nuktaForms: Map<Char, String>,
    val vowels: Map<Char, String>,
    val matras: Map<Char, String>,
    val signs: Map<Char, String>,
    val virama: Char,
    val nukta: Char,
    val addak: Char? = null,
    val digitZero: Char,
    val schwaDeletion: Boolean = true,
    val inherent: String = "a",
  ) {
    fun romanize(line: String, practical: Boolean = false): String {
      val sb = StringBuilder()
      var pendingA = false // inherent vowel written lazily, so word-final schwa can be dropped
      var syllables = 0 // vowel nuclei in the current word
      var afterConjunct = false // last consonant was preceded by a virama
      var viramaJustSeen = false
      var geminate = false // Gurmukhi addak doubles the next consonant
      fun flushA() {
        if (pendingA) sb.append(inherent); pendingA = false
      }
      fun endWord() {
        if (pendingA && (!schwaDeletion || syllables <= 1 || afterConjunct)) sb.append(inherent)
        pendingA = false; syllables = 0; afterConjunct = false; viramaJustSeen = false; geminate = false
      }
      var i = 0
      while (i < line.length) {
        val c = line[i]
        val consonant = consonants[c]
        when {
          consonant != null -> {
            flushA()
            var latin = consonant
            if (line.getOrNull(i + 1) == nukta) {
              latin = nuktaForms[c] ?: consonant; i++
            }
            // Practical spelling of the palatals (ISO c/ch → ch/chh), as Indian titles write them.
            if (practical) latin = PRACTICAL[latin] ?: latin
            if (geminate) sb.append(latin.first())
            sb.append(latin)
            afterConjunct = viramaJustSeen
            viramaJustSeen = false; geminate = false
            val next = line.getOrNull(i + 1)
            when {
              next == virama -> {
                viramaJustSeen = true; i++
              }
              next != null && next in matras -> {
                sb.append(matras.getValue(next)); syllables++; i++
              }
              else -> {
                pendingA = true; syllables++
              }
            }
          }
          c in vowels -> {
            flushA()
            // A bare carrier (ਅ ੳ ੲ, अ) followed by a matra is that matra's vowel.
            val next = line.getOrNull(i + 1)
            if (next != null && next in matras) {
              sb.append(matras.getValue(next)); i++
            } else {
              sb.append(vowels.getValue(c))
            }
            syllables++; afterConjunct = false
          }
          c in signs -> {
            flushA(); sb.append(signs.getValue(c))
          }
          c == addak -> {
            flushA(); geminate = true
          }
          // Stray mark with nothing to modify, or a joiner that only shapes the glyphs.
          c in matras || c == virama || c == nukta || c == '\u200C' || c == '\u200D' -> Unit
          c in digitZero..(digitZero + 9) -> {
            endWord(); sb.append('0' + (c - digitZero))
          }
          c == '।' || c == '॥' -> {
            endWord(); sb.append('.')
          }
          c in range && isMark(c) -> Unit // length marks and the like: no letter of their own
          c in range -> endWord() // unmapped script sign: drop rather than leak non-Latin
          else -> {
            endWord(); sb.append(c)
          }
        }
        i++
      }
      endWord()
      return sb.toString()
    }
  }

  private val PRACTICAL = mapOf("c" to "ch", "ch" to "chh")

  private val DEVANAGARI = Indic(
    range = '\u0900'..'\u097F',
    consonants = mapOf(
      'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ṅ",
      'च' to "c", 'छ' to "ch", 'ज' to "j", 'झ' to "jh", 'ञ' to "ñ",
      'ट' to "ṭ", 'ठ' to "ṭh", 'ड' to "ḍ", 'ढ' to "ḍh", 'ण' to "ṇ",
      'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
      'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
      'य' to "y", 'र' to "r", 'ल' to "l", 'ळ' to "ḷ", 'व' to "v",
      'श' to "ś", 'ष' to "ṣ", 'स' to "s", 'ह' to "h",
      // Precomposed nukta letters (U+0958..U+095F); NFC decomposes them, so both forms occur.
      '\u0958' to "q", '\u0959' to "k͟h", '\u095A' to "ġ", '\u095B' to "z",
      '\u095C' to "ṛ", '\u095D' to "ṛh", '\u095E' to "f", '\u095F' to "ẏ",
    ),
    nuktaForms = mapOf(
      'क' to "q", 'ख' to "k͟h", 'ग' to "ġ", 'ज' to "z", 'ड' to "ṛ", 'ढ' to "ṛh", 'फ' to "f", 'य' to "ẏ",
    ),
    vowels = mapOf(
      'अ' to "a", 'आ' to "ā", 'इ' to "i", 'ई' to "ī", 'उ' to "u", 'ऊ' to "ū",
      'ऋ' to "r̥", 'ॠ' to "r̥̄", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au",
      'ऍ' to "ê", 'ऑ' to "ô",
    ),
    matras = mapOf(
      'ा' to "ā", 'ि' to "i", 'ी' to "ī", 'ु' to "u", 'ू' to "ū", 'ृ' to "r̥", 'ॄ' to "r̥̄",
      'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au", 'ॅ' to "ê", 'ॉ' to "ô",
    ),
    signs = mapOf('ं' to "ṁ", 'ँ' to "m̐", 'ः' to "ḥ", 'ॐ' to "oṁ"),
    virama = '्',
    nukta = '़',
    digitZero = '०',
  )

  private val GURMUKHI = Indic(
    range = '\u0A00'..'\u0A7F',
    consonants = mapOf(
      'ਕ' to "k", 'ਖ' to "kh", 'ਗ' to "g", 'ਘ' to "gh", 'ਙ' to "ṅ",
      'ਚ' to "c", 'ਛ' to "ch", 'ਜ' to "j", 'ਝ' to "jh", 'ਞ' to "ñ",
      'ਟ' to "ṭ", 'ਠ' to "ṭh", 'ਡ' to "ḍ", 'ਢ' to "ḍh", 'ਣ' to "ṇ",
      'ਤ' to "t", 'ਥ' to "th", 'ਦ' to "d", 'ਧ' to "dh", 'ਨ' to "n",
      'ਪ' to "p", 'ਫ' to "ph", 'ਬ' to "b", 'ਭ' to "bh", 'ਮ' to "m",
      'ਯ' to "y", 'ਰ' to "r", 'ਲ' to "l", 'ਵ' to "v", 'ੜ' to "ṛ", 'ਸ' to "s", 'ਹ' to "h",
      '\u0A33' to "ḷ", '\u0A36' to "ś", '\u0A59' to "k͟h", '\u0A5A' to "ġ", '\u0A5B' to "z", '\u0A5E' to "f",
    ),
    nuktaForms = mapOf('ਸ' to "ś", 'ਲ' to "ḷ", 'ਖ' to "k͟h", 'ਗ' to "ġ", 'ਜ' to "z", 'ਫ' to "f"),
    vowels = mapOf(
      'ਅ' to "a", 'ਆ' to "ā", 'ਇ' to "i", 'ਈ' to "ī", 'ਉ' to "u", 'ਊ' to "ū",
      'ਏ' to "e", 'ਐ' to "ai", 'ਓ' to "o", 'ਔ' to "au", 'ੳ' to "u", 'ੲ' to "i",
    ),
    matras = mapOf(
      'ਾ' to "ā", 'ਿ' to "i", 'ੀ' to "ī", 'ੁ' to "u", 'ੂ' to "ū",
      'ੇ' to "e", 'ੈ' to "ai", 'ੋ' to "o", 'ੌ' to "au",
    ),
    signs = mapOf('ਂ' to "ṁ", 'ੰ' to "ṁ", 'ਃ' to "ḥ"),
    virama = '੍',
    nukta = '਼',
    addak = 'ੱ',
    digitZero = '੦',
  )

  /**
   * Bengali, Gujarati, Odia, Tamil, Telugu, Kannada and Malayalam share Devanagari's ISCII layout (the same letter
   * sits at the same offset in each 0x80 block), so their tables are Devanagari's shifted, plus each script's own
   * letters: short e/o (Dravidian), ழ/ഴ zh, ற/റ ṟ, ன ṉ, Malayalam chillus, Bengali khanda ta and yya.
   */
  private fun shifted(
    block: Int,
    schwaDeletion: Boolean,
    inherent: String = "a",
    extraConsonants: Map<Int, String> = emptyMap(),
    directSigns: Map<Char, String> = emptyMap(),
    initialA: String = "a",
  ): Indic {
    val delta = block - 0x0900
    fun <V> Map<Char, V>.shift() = entries.associate { (k, v) -> (k + delta) to v }
    fun Map<Int, String>.chars() = entries.associate { (k, v) -> (k + delta).toChar() to v }
    val shortEO = mapOf(0x090E to "e", 0x0912 to "o").chars()
    val shortEOMatras = mapOf(0x0946 to "e", 0x094A to "o").chars()
    return Indic(
      range = block.toChar()..(block + 0x7F).toChar(),
      consonants = DEVANAGARI.consonants.filterKeys { it.code < 0x0958 }.shift() +
        mapOf(0x0931 to "ṟ", 0x0929 to "ṉ", 0x0934 to "ḻ").chars() + extraConsonants.chars(),
      nuktaForms = DEVANAGARI.nuktaForms.entries.associate { (k, v) -> (k + delta) to v },
      vowels = DEVANAGARI.vowels.shift() + shortEO + mapOf(0x0905 to initialA).chars(),
      matras = DEVANAGARI.matras.shift() + shortEOMatras,
      signs = mapOf(0x0902 to "ṁ", 0x0901 to "m̐", 0x0903 to "ḥ").chars() + directSigns,
      virama = (0x094D + delta).toChar(),
      nukta = (0x093C + delta).toChar(),
      digitZero = (0x0966 + delta).toChar(),
      schwaDeletion = schwaDeletion,
      inherent = inherent,
    )
  }

  private val INDIC = listOf(
    // Bengali reads the inherent vowel as o (স্বপ্ন → shopno in store titles), and ব is b.
    shifted(0x0980, schwaDeletion = true, inherent = "o", initialA = "o", extraConsonants = mapOf(0x095C to "ṛ", 0x095D to "ṛh", 0x095F to "y"), directSigns = mapOf('\u09CE' to "t")),
    shifted(0x0A80, schwaDeletion = true),
    shifted(0x0B00, schwaDeletion = false, extraConsonants = mapOf(0x095C to "ṛ", 0x095D to "ṛh", 0x095F to "y")),
    shifted(0x0B80, schwaDeletion = false),
    shifted(0x0C00, schwaDeletion = false),
    shifted(0x0C80, schwaDeletion = false),
    // Malayalam chillus (U+0D7A..U+0D7F) are dead consonants, outside the shared layout.
    shifted(
      0x0D00, schwaDeletion = false,
      directSigns = mapOf('\u0D7A' to "ṇ", '\u0D7B' to "n", '\u0D7C' to "r", '\u0D7D' to "l", '\u0D7E' to "ḷ", '\u0D7F' to "k"),
    ),
  )

  // ---------------------------------------------------------------- Hangul (Revised Romanization)

  private object Hangul {
    private val INITIALS = listOf(
      "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h",
    )
    private val MEDIALS = listOf(
      "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi",
      "yu", "eu", "ui", "i",
    )

    // Final as written before a consonant or at the end (unreleased: 밥 → bap).
    private val FINALS = listOf(
      "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "l", "l", "l", "p", "l", "m", "p", "p", "t",
      "t", "ng", "t", "t", "k", "t", "p", "t",
    )

    // Final carried over onto a following silent ㅇ (한국어 → hangugeo).
    private val FINALS_AS_INITIAL = listOf(
      "", "g", "kk", "ks", "n", "nj", "nh", "d", "l", "lg", "lm", "lb", "ls", "lt", "lp", "lh", "m", "b",
      "bs", "s", "ss", "ng", "j", "ch", "k", "t", "p", "h",
    )
    private const val SILENT = 11
    private const val INIT_N = 2
    private const val INIT_R = 5
    private const val FIN_N = 4
    private const val FIN_L = 8

    // Nasalisation the RR spells out, keyed by (final, next initial).
    private val ASSIMILATIONS: Map<Pair<Int, Int>, String> = buildMap {
      for (f in listOf(1, 2, 24)) { put(f to INIT_N, "ng"); put(f to 6, "ng") } // 국물 gungmul
      for (f in listOf(17, 26)) { put(f to INIT_N, "m"); put(f to 6, "m") } // 입니다 imnida
      for (f in listOf(7, 19, 20, 22, 23, 25, 27)) { put(f to INIT_N, "n"); put(f to 6, "n") }
      put(FIN_N to INIT_R, "l") // 신라 silla
    }

    private fun decompose(c: Char): IntArray? {
      val o = c.code - 0xAC00
      if (o !in 0 until 11172) return null
      return intArrayOf(o / 588, (o % 588) / 28, o % 28)
    }

    fun romanize(line: String): String {
      val sb = StringBuilder()
      var prevFinal = 0
      for (i in line.indices) {
        val j = decompose(line[i])
        if (j == null) {
          sb.append(line[i]); prevFinal = 0; continue
        }
        val (initial, medial, final) = j
        // Liquidisation: ㄹ after ㄴ/ㄹ, and ㄴ after ㄹ, are both read l (몰라 molla, 설날 seollal).
        val liquid = (initial == INIT_R && (prevFinal == FIN_N || prevFinal == FIN_L)) ||
          (initial == INIT_N && prevFinal == FIN_L)
        sb.append(if (liquid) "l" else INITIALS[initial]).append(MEDIALS[medial])
        val next = line.getOrNull(i + 1)?.let(::decompose)
        sb.append(
          when {
            final == 0 -> ""
            next == null -> FINALS[final]
            next[0] == SILENT -> FINALS_AS_INITIAL[final]
            final == FIN_L && (next[0] == INIT_R || next[0] == INIT_N) -> "l"
            else -> ASSIMILATIONS[final to next[0]] ?: FINALS[final]
          },
        )
        // A final that moved onto the next syllable no longer conditions its initial.
        prevFinal = if (next != null && next[0] == SILENT) 0 else final
      }
      return sb.toString()
    }
  }

  // ---------------------------------------------------------------- Kana (Hepburn)

  private object Kana {
    private val YOUON = mapOf(
      "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo", "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
      "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho", "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
      "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo", "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
      "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo", "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
      "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo", "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
      "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo", "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe",
      "ふぉ" to "fo", "うぃ" to "wi", "うぇ" to "we", "てぃ" to "ti", "でぃ" to "di", "とぅ" to "tu",
      "どぅ" to "du", "ゔぁ" to "va", "ゔぃ" to "vi", "ゔぇ" to "ve", "ゔぉ" to "vo", "しぇ" to "she",
      "ちぇ" to "che", "じぇ" to "je",
    )
    private val KANA = mapOf(
      'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
      'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
      'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
      'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
      'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
      'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
      'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
      'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
      'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
      'わ' to "wa", 'を' to "o", 'ん' to "n",
      'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
      'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
      'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
      'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
      'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
      'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
      'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゔ' to "vu",
      '・' to " ", '、' to ",", '。' to ".",
    )

    private fun syllable(s: String, i: Int): Pair<String, Int>? {
      if (i >= s.length) return null
      if (i + 1 < s.length) YOUON[s.substring(i, i + 2)]?.let { return it to 2 }
      return KANA[s[i]]?.let { it to 1 }
    }

    fun romanize(text: String): String {
      // Katakana sits 0x60 above hiragana, so one table serves both.
      val s = buildString { text.forEach { append(if (it in '\u30A1'..'\u30F6') it - 0x60 else it) } }
      val sb = StringBuilder()
      var i = 0
      while (i < s.length) {
        val c = s[i]
        when {
          c == 'っ' -> { // sokuon doubles the next consonant (きって → kitte)
            syllable(s, i + 1)?.first?.firstOrNull()?.takeIf { it !in "aiueo" }?.let(sb::append)
            i++
          }
          c == 'ー' -> { // chouon lengthens the previous vowel (ラーメン → raamen)
            sb.lastOrNull()?.takeIf { it in "aiueo" }?.let(sb::append)
            i++
          }
          else -> {
            val (latin, len) = syllable(s, i) ?: (c.toString() to 1)
            sb.append(latin); i += len
          }
        }
      }
      return sb.toString()
    }
  }

  // ---------------------------------------------------------------- Cyrillic

  private object Cyrillic {
    private val BASE = mapOf(
      'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ж' to "zh", 'з' to "z",
      'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p",
      'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch",
      'ш' to "sh", 'щ' to "shch", 'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
      'ё' to "yo",
    )

    // Marker letters only one language uses, most distinctive first (Macedonian before Serbian,
    // which shares ј љ њ џ with it; Ukrainian before Belarusian, which shares і). Anything else,
    // Bulgarian included, is read as Russian.
    private const val UKRAINIAN_MARKERS = "їєґ"

    private val LANGUAGES: List<Pair<String, Map<Char, String>>> = listOf(
      "ѓќѕ" to mapOf(
        'ѓ' to "gj", 'ќ' to "kj", 'ѕ' to "dz", 'ј' to "j", 'љ' to "lj", 'њ' to "nj", 'џ' to "dž",
        'ж' to "ž", 'ч' to "č", 'ш' to "š", 'х' to "h", 'ц' to "c",
      ),
      "ђћџљњј" to mapOf(
        'ђ' to "đ", 'ј' to "j", 'љ' to "lj", 'њ' to "nj", 'ћ' to "ć", 'џ' to "dž", 'ж' to "ž",
        'ч' to "č", 'ш' to "š", 'х' to "h", 'ц' to "c",
      ),
      "ңөү" to mapOf('ң' to "ng", 'ө' to "ö", 'ү' to "ü"),
      UKRAINIAN_MARKERS to mapOf('г' to "h", 'ґ' to "g", 'и' to "y", 'і' to "i", 'ї' to "yi", 'є' to "ye"),
      "ўі" to mapOf('г' to "h", 'ў' to "ŭ", 'і' to "i"),
    )

    // Ukrainian (2010 national system) writes these with a y only at the start of a word.
    private val UKRAINIAN_MEDIAL = mapOf('ї' to "i", 'є' to "ie", 'ю' to "iu", 'я' to "ia", 'й' to "i")

    // Russian е ё ю я take a leading y word-initially and after a vowel or sign (BGN/PCGN).
    private val RUSSIAN_IOTATED = mapOf('е' to "ye", 'ё' to "yo", 'ю' to "yu", 'я' to "ya")
    private const val RUSSIAN_VOWELS_AND_SIGNS = "аеёиоуыэюяьъ"

    fun romanize(line: String): String {
      val lower = line.lowercase()
      val language = LANGUAGES.firstOrNull { (markers, _) -> lower.any { it in markers } }
      val overrides = language?.second
      val table = if (overrides == null) BASE else BASE + overrides
      val ukrainian = language?.first == UKRAINIAN_MARKERS
      val sb = StringBuilder()
      line.forEachIndexed { i, c ->
        val l = c.lowercaseChar()
        val prev = line.getOrNull(i - 1)?.lowercaseChar()
        val wordInitial = prev == null || prev !in '\u0400'..'\u04FF'
        val mapped = when {
          overrides == null && l in RUSSIAN_IOTATED && (wordInitial || prev in RUSSIAN_VOWELS_AND_SIGNS) ->
            RUSSIAN_IOTATED.getValue(l)
          ukrainian && !wordInitial && l in UKRAINIAN_MEDIAL -> UKRAINIAN_MEDIAL.getValue(l)
          else -> table[l]
        }
        when {
          mapped == null -> sb.append(c)
          c.isUpperCase() -> sb.append(mapped.replaceFirstChar { it.uppercaseChar() })
          else -> sb.append(mapped)
        }
      }
      return sb.toString()
    }
  }

  // ---------------------------------------------------------------- Greek (ELOT 743, simplified)

  private object Greek {
    private val DIGRAPHS = mapOf(
      "ου" to "ou", "ού" to "ou", "αι" to "ai", "αί" to "ai", "ει" to "ei", "εί" to "ei", "οι" to "oi", "οί" to "oi",
      "αυ" to "av", "αύ" to "av", "ευ" to "ev", "εύ" to "ev", "γγ" to "ng", "γκ" to "gk", "γξ" to "nx", "γχ" to "nch",
    )
    private val LETTERS = mapOf(
      'α' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e", 'ζ' to "z", 'η' to "i", 'θ' to "th", 'ι' to "i",
      'κ' to "k", 'λ' to "l", 'μ' to "m", 'ν' to "n", 'ξ' to "x", 'ο' to "o", 'π' to "p", 'ρ' to "r", 'σ' to "s",
      'ς' to "s", 'τ' to "t", 'υ' to "y", 'φ' to "f", 'χ' to "ch", 'ψ' to "ps", 'ω' to "o", 'ά' to "a", 'έ' to "e",
      'ή' to "i", 'ί' to "i", 'ό' to "o", 'ύ' to "y", 'ώ' to "o", 'ϊ' to "i", 'ϋ' to "y", 'ΐ' to "i", 'ΰ' to "y",
    )

    fun romanize(line: String): String {
      val sb = StringBuilder()
      var i = 0
      while (i < line.length) {
        val c = line[i]
        val pair = if (i + 1 < line.length) line.substring(i, i + 2).lowercase() else ""
        val di = DIGRAPHS[pair]
        val latin = di ?: LETTERS[c.lowercaseChar()]
        when {
          latin == null -> sb.append(c)
          c.isUpperCase() -> sb.append(latin.replaceFirstChar { it.uppercaseChar() })
          else -> sb.append(latin)
        }
        i += if (di != null) 2 else 1
      }
      return sb.toString()
    }
  }

  // ---------------------------------------------------------------- Thai (RTGS, rule-based)

  /**
   * ponytail: RTGS without a lexicon. Leading vowels move after their consonant (cluster), a consonant pair with no
   * vowel takes an implicit o (ผล → phon), finals use their final value; tone marks and karan-silenced letters drop.
   * Implicit a between syllables (สบาย sabai) and true vowel length are guesses; good enough for name matching.
   */
  private object Thai {
    private val INITIAL = mapOf(
      'ก' to "k", 'ข' to "kh", 'ฃ' to "kh", 'ค' to "kh", 'ฅ' to "kh", 'ฆ' to "kh", 'ง' to "ng", 'จ' to "ch",
      'ฉ' to "ch", 'ช' to "ch", 'ซ' to "s", 'ฌ' to "ch", 'ญ' to "y", 'ฎ' to "d", 'ฏ' to "t", 'ฐ' to "th", 'ฑ' to "th",
      'ฒ' to "th", 'ณ' to "n", 'ด' to "d", 'ต' to "t", 'ถ' to "th", 'ท' to "th", 'ธ' to "th", 'น' to "n", 'บ' to "b",
      'ป' to "p", 'ผ' to "ph", 'ฝ' to "f", 'พ' to "ph", 'ฟ' to "f", 'ภ' to "ph", 'ม' to "m", 'ย' to "y", 'ร' to "r",
      'ฤ' to "rue", 'ล' to "l", 'ฦ' to "lue", 'ว' to "w", 'ศ' to "s", 'ษ' to "s", 'ส' to "s", 'ห' to "h", 'ฬ' to "l",
      'อ' to "", 'ฮ' to "h",
    )
    private val FINAL = mapOf(
      'ก' to "k", 'ข' to "k", 'ค' to "k", 'ฆ' to "k", 'ง' to "ng", 'จ' to "t", 'ช' to "t", 'ซ' to "t", 'ฌ' to "t",
      'ญ' to "n", 'ฎ' to "t", 'ฏ' to "t", 'ฐ' to "t", 'ฑ' to "t", 'ฒ' to "t", 'ณ' to "n", 'ด' to "t", 'ต' to "t",
      'ถ' to "t", 'ท' to "t", 'ธ' to "t", 'น' to "n", 'บ' to "p", 'ป' to "p", 'ผ' to "p", 'พ' to "p", 'ฟ' to "p",
      'ภ' to "p", 'ฝ' to "p", 'ม' to "m", 'ย' to "i", 'ร' to "n", 'ล' to "n", 'ว' to "o", 'ศ' to "t", 'ษ' to "t",
      'ส' to "t", 'ฬ' to "n", 'ห' to "", 'อ' to "", 'ฮ' to "",
    )
    private val AFTER = mapOf(
      'ะ' to "a", '\u0E31' to "a", '\u0E32' to "a", '\u0E33' to "am", '\u0E34' to "i", '\u0E35' to "i", '\u0E36' to "ue", '\u0E37' to "ue", '\u0E38' to "u",
      '\u0E39' to "u", '\u0E45' to "",
    )
    private val LEADING = mapOf('\u0E40' to "e", '\u0E41' to "ae", '\u0E42' to "o", '\u0E43' to "ai", '\u0E44' to "ai")
    private const val CLUSTER_SECOND = "รลว"
    private const val SONORANTS = "งญนมยรลว"

    private fun isConsonant(c: Char?) = c != null && c in INITIAL

    fun romanize(line: String): String {
      // Drop tone marks and maitaikhu; drop a letter silenced by karan (์), with its vowel sign.
      val s = Regex("[\\u0E01-\\u0E2E][\\u0E34\\u0E38]?\\u0E4C").replace(line, "").filter { it !in "\u0E48\u0E49\u0E4A\u0E4B\u0E47\u0E46\u0E2F" }
      val sb = StringBuilder()
      var i = 0
      // Writes the onset at j (a silent ห before a sonorant, then a consonant and an optional ร/ล/ว cluster
      // letter) and returns the index after it. After a leading vowel a cluster letter needs no vowel sign of its own.
      fun initialAt(j: Int, leading: Boolean): Int {
        var k = j
        if (s[k] == 'ห' && s.getOrNull(k + 1)?.let { it in SONORANTS } == true) k++
        sb.append(INITIAL.getValue(s[k])); k++
        val second = s.getOrNull(k)
        if (second != null && second in CLUSTER_SECOND && (s.getOrNull(k + 1) in AFTER || leading && second != 'ว' && isConsonant(s.getOrNull(k + 1)))) {
          sb.append(INITIAL.getValue(second)); k++
        }
        return k
      }
      fun finalAt(j: Int): Int {
        if (!s.isFinalAt(j)) return j
        sb.append(FINAL[s[j]] ?: ""); return j + 1
      }
      while (i < s.length) {
        val c = s[i]
        when {
          c in LEADING && isConsonant(s.getOrNull(i + 1)) -> {
            var k = initialAt(i + 1, leading = true)
            val v = when {
              c == '\u0E40' && s.getOrNull(k) == '\u0E32' -> { k++; if (s.getOrNull(k) == 'ะ') k++; "ao" }
              c == '\u0E40' && s.getOrNull(k) == '\u0E35' && s.getOrNull(k + 1) == 'ย' -> { k += 2; "ia" }
              c == '\u0E40' && s.getOrNull(k) == '\u0E37' && s.getOrNull(k + 1) == 'อ' -> { k += 2; "uea" }
              c == '\u0E40' && s.getOrNull(k) == 'อ' -> { k++; "oe" }
              c == '\u0E40' && s.getOrNull(k) == '\u0E34' -> { k++; "oe" }
              else -> { if (s.getOrNull(k) == 'ะ') k++; LEADING.getValue(c) }
            }
            sb.append(v)
            i = finalAt(k)
          }
          isConsonant(c) -> {
            var k = initialAt(i, leading = false)
            val next = s.getOrNull(k)
            when {
              next == '\u0E31' && s.getOrNull(k + 1) == 'ว' -> { sb.append("ua"); i = finalAt(k + 2) }
              next == '\u0E37' && s.getOrNull(k + 1) == 'อ' -> { sb.append("ue"); i = finalAt(k + 2) }
              next != null && next in AFTER -> {
                sb.append(AFTER.getValue(next)); k++
                if (s.getOrNull(k) == 'ะ' || s.getOrNull(k) == '\u0E32') k++
                i = finalAt(k)
              }
              next == 'ว' && isConsonant(s.getOrNull(k + 1)) -> { sb.append("ua"); i = finalAt(k + 1) }
              next == 'อ' -> { sb.append("o"); i = finalAt(k + 1) }
              isConsonant(next) && s.isFinalAt(k) -> { sb.append("o"); i = finalAt(k) }
              isConsonant(next) -> { sb.append("a"); i = k }
              else -> i = k
            }
          }
          c in THAI && c in '\u0E50'..'\u0E59' -> { sb.append('0' + (c - '๐')); i++ }
          c in THAI -> i++
          else -> { sb.append(c); i++ }
        }
      }
      return sb.toString()
    }

    // A consonant closes the syllable when no vowel sign follows it and it does not start a new onset.
    private fun String.isFinalAt(j: Int): Boolean {
      val c = getOrNull(j) ?: return false
      if (!isConsonant(c)) return false
      val after = getOrNull(j + 1)
      if (after in AFTER || after == 'อ' || after == 'ว' && isConsonant(getOrNull(j + 2))) return false
      // Do not close onto a consonant that would then be left alone at the end of the word (เหตุผล: ผ opens ผล).
      return !(isConsonant(after) && getOrNull(j + 2).let { it == null || it !in INITIAL && it !in AFTER && it != 'อ' })
    }
  }

  // ---------------------------------------------------------------- Abjads (Arabic/Persian/Urdu, Hebrew)

  /** Consonants plus written long vowels and pointing; unpointed short vowels cannot be recovered. */
  private object Abjad {
    private val ARABIC = mapOf(
      'ا' to "a", 'أ' to "a", 'إ' to "i", 'آ' to "a", 'ٱ' to "a", 'ء' to "", 'ؤ' to "u", 'ئ' to "i", 'ب' to "b",
      'ت' to "t", 'ث' to "th", 'ج' to "j", 'ح' to "h", 'خ' to "kh", 'د' to "d", 'ذ' to "dh", 'ر' to "r", 'ز' to "z",
      'س' to "s", 'ش' to "sh", 'ص' to "s", 'ض' to "d", 'ط' to "t", 'ظ' to "z", 'ع' to "", 'غ' to "gh", 'ف' to "f",
      'ق' to "q", 'ك' to "k", 'ل' to "l", 'م' to "m", 'ن' to "n", 'ه' to "h", 'ة' to "a", 'ى' to "a",
      // Persian and Urdu letters.
      'پ' to "p", 'چ' to "ch", 'ژ' to "zh", 'گ' to "g", 'ک' to "k", 'ۀ' to "h", 'ٹ' to "t", 'ڈ' to "d",
      'ڑ' to "r", 'ں' to "n", 'ے' to "e", 'ھ' to "h", 'ہ' to "h", 'ۃ' to "a", 'ۓ' to "e",
      // Pointing, when present.
      '\u064E' to "a", '\u0650' to "i", '\u064F' to "u", '\u064B' to "an", '\u064D' to "in", '\u064C' to "un",
    )
    private val HEBREW = mapOf(
      'ג' to "g", 'ד' to "d", 'ה' to "h", 'ז' to "z", 'ח' to "ch", 'ט' to "t",
      'ך' to "ch", 'ל' to "l", 'ם' to "m", 'מ' to "m", 'ן' to "n", 'נ' to "n", 'ס' to "s", 'ע' to "", 'ף' to "f",
      'צ' to "ts", 'ץ' to "ts", 'ק' to "k", 'ר' to "r", 'ש' to "sh", 'ת' to "t",
    )
    private val ARABIC_WAW_YA = mapOf('و' to ("w" to "u"), 'ي' to ("y" to "i"), 'ی' to ("y" to "i"))
    private val HEBREW_MATRES = mapOf('ו' to ("v" to "o"), 'י' to ("y" to "i"), 'א' to ("a" to ""), 'ע' to ("a" to ""))

    // ب כ פ are stops word-initially and spirants elsewhere (no dagesh in unpointed text).
    private val HEBREW_BKP = mapOf('ב' to ("b" to "v"), 'כ' to ("k" to "ch"), 'פ' to ("p" to "f"))

    // ponytail: و/ي read as w/y word-initially and as u/i inside a word; a consonantal medial w/y is misread.
    fun arabic(run: String): String = buildString {
      run.forEachIndexed { i, c ->
        val wordInitial = i == 0 || run[i - 1] !in '\u0620'..'\u06D3'
        val wy = ARABIC_WAW_YA[c]
        when {
          wy != null -> append(if (wordInitial) wy.first else wy.second)
          c == '\u0651' -> lastOrNull()?.takeIf { it.isLetter() }?.let(::append) // shadda doubles
          c in '\u0660'..'\u0669' -> append('0' + (c - '\u0660'))
          c in '\u06F0'..'\u06F9' -> append('0' + (c - '\u06F0'))
          else -> append(ARABIC[c] ?: if (isMark(c) || c == '\u0640') "" else c.toString())
        }
      }
    }

    // ponytail: unpointed Hebrew; ו/י are vowels inside a word, word-final ה is a, א/ע open a word with a.
    fun hebrew(run: String): String = buildString {
      run.forEachIndexed { i, c ->
        val wordInitial = i == 0 || run[i - 1] !in '\u05D0'..'\u05EA'
        val wordFinal = run.getOrNull(i + 1)?.let { it !in '\u05D0'..'\u05EA' } ?: true
        val bkp = HEBREW_BKP[c]
        val mater = HEBREW_MATRES[c]
        when {
          bkp != null -> append(if (wordInitial) bkp.first else bkp.second)
          mater != null -> append(if (wordInitial || c == 'ו' && run.getOrNull(i + 1) == 'ו') mater.first else mater.second)
          c == 'ה' && wordFinal && !wordInitial -> append('a')
          else -> append(HEBREW[c] ?: if (isMark(c) || c in '\u05B0'..'\u05C7') "" else c.toString())
        }
      }
    }
  }
}
