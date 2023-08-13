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
