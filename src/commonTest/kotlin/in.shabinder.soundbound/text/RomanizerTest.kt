package `in`.shabinder.soundbound.text

import kotlin.test.Test
import kotlin.test.assertEquals

class RomanizerTest {
  private fun golden(vararg cases: Pair<String, String?>) =
    cases.forEach { (line, expected) -> assertEquals(expected, Romanizer.romanize(line), line) }

  @Test fun devanagariIso15919() = golden(
    "नमस्ते" to "namaste",
    "तुझे देखा तो ये जाना सनम" to "tujhe dekhā to ye jānā sanam",
    "दिल" to "dil", // word-final schwa dropped
    "हम" to "ham",
    "मित्र" to "mitra", // kept after a conjunct
    "क्या" to "kyā",
    "कृष्ण" to "kr̥ṣṇa",
    "ज़िंदगी" to "ziṁdagī", // combining nukta
    "ज़िंदगी" to "ziṁdagī", // precomposed nukta letter
    "मैं" to "maiṁ",
    "प्यार है।" to "pyār hai.",
  )

  @Test fun gurmukhiIso15919() = golden(
    "ਸਤ ਸ੍ਰੀ ਅਕਾਲ" to "sat srī akāl",
    "ਪੰਜਾਬ" to "paṁjāb",
    "ਪੱਗ" to "pagg", // addak doubles
    "ਇੱਕ" to "ikk",
    "ਕੁੜੀ" to "kuṛī",
    "ਗੁਰੂ" to "gurū",
  )

  @Test fun hangulRevisedRomanization() = golden(
    "사랑해" to "saranghae",
    "한국어" to "hangugeo", // liaison
    "안녕하세요" to "annyeonghaseyo",
    "국물" to "gungmul", // nasalisation
    "감사합니다" to "gamsahamnida",
    "신라" to "silla",
    "몰라" to "molla", // liquidisation
    "설날" to "seollal",
  )

  @Test fun kanaHepburn() = golden(
    "ありがとう" to "arigatou",
    "きって" to "kitte", // sokuon
    "ラーメン" to "raamen", // chouon
    "ちょっと" to "chotto", // youon + sokuon
    "東京へ行く" to null, // kanji needs a dictionary: out of scope
  )

  @Test fun cyrillic() = golden(
    "Привет, мир" to "Privet, mir",
    "Я тебя люблю" to "Ya tebya lyublyu",
    "Дмитриев" to "Dmitriyev",
    "Київ" to "Kyiv",
    "Україна" to "Ukraina",
    "Ђорђе Балашевић" to "Đorđe Balašević",
  )

  @Test fun nothingToShowForLatinBlankOrHan() = golden(
    "Hello world" to null,
    "" to null,
    "♪" to null,
    "我爱你" to null,
  )

  private fun latin(vararg cases: Pair<String, String>) =
    cases.forEach { (text, expected) -> assertEquals(expected, Romanizer.latin(text), text) }

  @Test fun latinIndic() = latin(
    "तुम ही हो" to "tum hi ho",
    "केसरिया" to "kesariya",
    "चन्ना मेरेया" to "channa mereya",
    "सिंह" to "singh",
    "ਲੰਬੜਗਿਨੀ" to "lambaragini", // medial schwa kept: no lexicon,
    "ਸਤ ਸ੍ਰੀ ਅਕਾਲ" to "sat sri akal",
    "স্বপ্নের পৃথিবী" to "sbopner prithibi",
    "હજુ રસભર રાત" to "haju rasabhar rat",
    "கண்ணான கண்ணே" to "kannana kanne",
    "ఇంకేం ఇంకేం" to "inken inken",
    "ಬಾವಿ ಕಪ್ಪೆ" to "bavi kappe",
    "മലയാളം" to "malayalan",
  )

  @Test fun latinEastAsian() = latin(
    "사랑해" to "saranghae",
    "ありがとう" to "arigatou",
    "月亮代表我的心" to "yue liang dai biao wo de xin",
    "別愛我" to "bie ai wo", // Traditional and Simplified share a reading
    "别爱我" to "bie ai wo",
    "周杰倫 Jay" to "zhou jie lun Jay",
    "東京へ行く" to "東京he行ku", // kanji have no reading without a dictionary
  )

  @Test fun latinOtherScripts() = latin(
    "Камин" to "Kamin",
    "Ξένος" to "Xenos",
    "Ότι πω" to "Oti po",
    "Αν η μισή μου καρδιά" to "An i misi mou kardia",
    "ไม่มีเหตุผล" to "maimihetuphon",
    "บุพเพสันนิวาส" to "bupphesanniwat",
    "بغداد" to "bghdad",
    "فاصله" to "faslh",
    "אפור" to "afor",
    "Beyoncé" to "Beyoncé",
  )
}
