package `in`.shabinder.soundbound.matcher

import kotlin.jvm.JvmName


@JvmName("stringSluggify")
fun String.sluggify(): String {
    return sluggify(this)
}

fun sluggify(string: String): String {
    return string.lowercase()
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
        .replace(",", " ")
        .replace(" ", "-")
        .trim { it.isWhitespace() || it == '-' || it == '_' }
}
