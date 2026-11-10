package `in`.shabinder.soundbound.matcher

/**
 * Printable-ASCII form of the alias table, so Kotlin/JS emits the literal as is instead of as \uXXXX escapes (6 bytes
 * a character, and a zipline carries each literal three times). Keys use only a-z, 0-9 and non-ASCII letters. ASCII
 * passes through ('\n' becomes ';'); a non-ASCII character is '~' plus three base-52 digits of its code, or, right
 * after another non-ASCII character, one digit of the difference when that is within -25..26 (a Cyrillic or kana word).
 */
internal object ArtistAliasCodec {
    private const val D = "!#%&'()*+,-./:<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`{}"
    private const val B = 52

    fun encode(text: String): String {
        require(D.length == B)
        val sb = StringBuilder(text.length)
        var prev = -1
        for (c in text) {
            val o = c.code
            when {
                c == '\n' -> { sb.append(';'); prev = -1 }
                o < 128 -> { require(c !in D && c != '~' && c != ';' && c != '"' && c != '\\' && c != '$') { "unencodable '$c'" }; sb.append(c); prev = -1 }
                prev >= 0 && o - prev in -25..26 -> { sb.append(D[o - prev + 25]); prev = o }
                else -> { sb.append('~').append(D[o / (B * B)]).append(D[o / B % B]).append(D[o % B]); prev = o }
            }
        }
        return sb.toString()
    }

    fun decode(text: String): String {
        val sb = StringBuilder(text.length)
        var prev = -1
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val o = when {
                c == '~' -> {
                    val code = (D.indexOf(text[i + 1]) * B + D.indexOf(text[i + 2])) * B + D.indexOf(text[i + 3])
                    i += 3
                    code
                }
                prev >= 0 && D.indexOf(c) >= 0 -> prev + D.indexOf(c) - 25
                else -> { sb.append(if (c == ';') '\n' else c); prev = -1; i++; continue }
            }
            sb.append(o.toChar()); prev = o; i++
        }
        return sb.toString()
    }
}
