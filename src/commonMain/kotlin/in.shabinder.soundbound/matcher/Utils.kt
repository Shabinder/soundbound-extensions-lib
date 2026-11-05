package `in`.shabinder.soundbound.matcher

import kotlin.jvm.JvmName


@JvmName("stringSluggify")
fun String.sluggify(): String {
    return sluggify(this)
}

fun sluggify(string: String): String {
    return string.lowercase()
//        .replace("-", "") // join names, so we don't split single name into two later
        .replace("'", "")
        .replace("\"", "")
        .replace(".", " ")
        .replace("_", "-")
        .replace("(", " ")
        .replace(")", " ")
        .replace("[", " ")
        .replace("]", " ")
        .replace("{", " ")
        .replace("}", " ")
        .replace(";", " ")
        .replace(":", " ")
        .replace(", ", " ") // special case: so we don't get `--` in the slug
        .replace(",", " ")
        .replace(" ", "-")
        .trim { it.isWhitespace() || it == '-' || it == '_' }
}


// ponytail: no Unicode NFD in commonMain (JS/iOS), so a Latin table; extend when a catalogue needs more.
private const val ACCENTED = "àáâãäåāăąçćčďèéêëēėęěìíîïīįñńňòóôõöøōőùúûüūůűųýÿžźżśšşřłğ"
private const val PLAIN = "aaaaaaaaacccdeeeeeeeeiiiiiinnnoooooooouuuuuuuuyyzzzsssrlg"

/** é -> e, ü -> u, … for common Latin letters; anything else is returned unchanged. */
fun foldDiacritics(text: String): String = buildString(text.length) {
    for (c in text) {
        val i = ACCENTED.indexOf(c.lowercaseChar())
        append(if (i < 0) c else if (c.isUpperCase()) PLAIN[i].uppercaseChar() else PLAIN[i])
    }
}

/** Bracket groups that open with one of these are promotional clutter, not a version marker. */
private val PROMOTIONAL_OPENERS = setOf(
    "feat", "ft", "featuring", "with", "official", "video", "audio", "music", "lyrics", "lyric",
    "visualizer", "visualiser", "hd", "hq", "4k", "remaster", "remastered", "explicit", "clean",
    "original", "album", "radio", "edit", "single", "bonus", "deluxe", "from",
)
private val BRACKET_GROUP = Regex("""[(\[][^)\]]*[)\]]""")
private val FEATURING_TAIL = Regex("""\s+(ft\.?|feat\.?|featuring)\s+.*$""", RegexOption.IGNORE_CASE)

/**
 * Shared search-query title cleanup: drops promotional bracket groups ("(Official Video)",
 * "(From \"Film\")", "[Remastered 2011]") and "feat." tails, folds diacritics and collapses
 * whitespace. Version groups ("(VIP)", "(Acoustic)") stay, unbracketed, because they pick the
 * recording. Never returns an empty string for a non-blank title.
 *
 * Reimplemented from the behaviour of melodybridge SearchTerms.CleanTitle (AGPL-3.0, no code copied).
 */
fun cleanSearchTitle(title: String): String {
    val cleaned = foldDiacritics(
        BRACKET_GROUP.replace(title) { group ->
            val inner = group.value.trim('(', ')', '[', ']', ' ')
            val opener = inner.takeWhile { it.isLetterOrDigit() }.lowercase()
            if (opener in PROMOTIONAL_OPENERS) " " else " $inner "
        }.replace(FEATURING_TAIL, " ")
    ).split(' ', '\t', '\n').filter { it.isNotBlank() }.joinToString(" ")
    return cleaned.ifEmpty { foldDiacritics(title).split(' ').filter { it.isNotBlank() }.joinToString(" ") }
}
