package `in`.shabinder.soundbound.matcher

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Real cross-script pairs from the 2026-11-10 spike (MusicBrainz transliterated tracklists, MusicBrainz ISRC vs the
 * store track with that ISRC, YouTube Music native-script searches). Accepted pairs are the same recording; rejected
 * pairs are another song by the same credits at the same length, so only the title can tell them apart.
 */
class CrossScriptMatchTest {
    private data class Case(val t: String, val a: List<String>, val d: Long, val ct: String, val ca: List<String>, val cd: Long)

    private val same = listOf(
        Case("অলস-রোগ", listOf("Nemesis"), 321, "Olosh Roug", listOf("Nemesis"), 319),
        Case("সময় নেই আর", listOf("Nemesis"), 371, "Shomoy Nei Aar", listOf("Nemesis"), 369),
        Case("আবার দেখা হবে কি?", listOf("Nemesis"), 221, "Abar Dekha Hobe", listOf("Nemesis"), 221),
        Case("হাজার বছর", listOf("Nemesis"), 323, "Hajar Bochhor", listOf("Nemesis"), 322),
        Case("Вулиця (Може все не так)", listOf("Океан Ельзи"), 326, "Вулиця (може все не так)", listOf("Okean Elzy"), 326),
        Case("Холодные огни шереметьево‒2", listOf("Александр Буйнов"), 240, "Холодные огни Шереметьево-2", listOf("Александр Буйнов"), 240),
        Case("Мальчик-бродяга", listOf("Андрей Губин"), 192, "Мальчик бродяга", listOf("Андрей Губин"), 193),
        Case("Мы с тобою ‒ “герои”", listOf("Александр Буйнов"), 186, "Мы с тобою - герои", listOf("Александр Буйнов"), 186),
        Case("सरीवर सर", listOf("संदीप खरे"), 253, "Sariwar Sar", listOf("Sandeep Khare"), 253),
        Case("सैरभर झाला सारा वारा", listOf("शैलेष रानडे"), 324, "Sairbhar Jhala Sara Wara", listOf("Shailesh Ranade"), 324),
        Case("चम्हालातु चंगतोसा", listOf("संदीप खरे"), 266, "Chamala Tu Changto Sa", listOf("Salil Kulkarni", "Sandeep Khare", "Mithilesh", "Anjali Kulkarni", "Prajanli Barve"), 267),
        Case("बुंबुंबा...", listOf("सलील कुलकर्णी", "संदीप खरे"), 332, "Boom Boom Ba", listOf("Salil Kulkarni", "Sandeep Khare", "Mithilesh", "Anjali Kulkarni", "Prajanli Barve"), 332),
        Case("파라다이스 (Limited ver.)", listOf("DIA"), 223, "Paradise (Limited Version)", listOf("Dia"), 223),
        Case("LO OK (빈챈현스S ver.)", listOf("DIA"), 219, "LO OK (빈챈현스S Version)", listOf("Dia"), 219),
        Case("몰라몰라 (inst.)", listOf("타히티"), 198, "몰라몰라 Instrumental", listOf("Tahiti"), 198),
        Case("키스해줘 (빈챈현스S ver.)", listOf("DIA"), 187, "Kiss me (빈챈현스S Version)", listOf("Dia"), 187),
        Case("為了你，為了我", listOf("Beyond"), 279, "Wei Liao Ni, Wei Liao Wo", listOf("Beyond"), 280),
        Case("月影のナポリ", listOf("W"), 180, "月影のナポリ (TINTARELLA DI LUNA)", listOf("W"), 180),
        Case("あいのうた 〜Swallowtail Butterfly〜", listOf("My Little Lover"), 318, "Ai no Uta -Swallowtail Butterfly-", listOf("My Little Lover"), 317),
        Case("悲しき16才", listOf("W"), 121, "悲しき16才 (HEARTACHES AT SWEET SIXTEEN)", listOf("W"), 121),
        Case("NOW AND THEN 〜失われた時を求めて〜", listOf("My Little Lover"), 245, "NOW AND THEN", listOf("My Little Lover"), 243),
        Case("ビー・ミーン", listOf("DNCE"), 211, "Be Mean", listOf("DNCE"), 211),
        Case("セレンディピティ", listOf("Li-sa-X"), 153, "Serendipity", listOf("Li-sa-X"), 152),
        Case("アンスウィート", listOf("DNCE"), 200, "Unsweet", listOf("DNCE"), 200),
        Case("ペイ・マイ・レント", listOf("DNCE"), 193, "Pay My Rent", listOf("DNCE"), 193),
        Case("محمد بو يا محمد", listOf("إلهام المدفعي"), 353, "Mohamad bouya Mohamad", listOf("إلهام المدفعي"), 353),
        Case("ون يا قلب", listOf("إلهام المدفعي"), 407, "Win ya galob", listOf("إلهام المدفعي"), 407),
        Case("عيد يا محبوبي", listOf("إلهام المدفعي"), 337, "Eidi ya mahbobi", listOf("إلهام المدفعي"), 337),
        Case("عيرتني بالشيب", listOf("ناظم الغزالي"), 0, "Ayyaratni Bil Sheyb", listOf("Nazem Alghazali"), 0),
        Case("এক মুঠো রোদ্দুর", listOf("Balam"), 322, "Ek Mutho Roddur", listOf("Balam"), 322),
        Case("না ছুঁই না ছুঁইলে তারে", listOf("Kanak Chapa", "S.I. Tutul"), 314, "Na Chui Na Chuile Tore", listOf("Kanak Chapa", "S.I. Tutul"), 314),
        Case("হৃদয়ের ঠিকানা", listOf("Balam", "Julee"), 268, "Hridoyer Thikana", listOf("Balam", "Julee"), 268),
        Case("তুমিহীনা", listOf("Balam", "Julee"), 311, "Tumihina", listOf("Balam", "Julee"), 311),
        Case("Νάμαι πάλι εδώ ζωντανός", listOf("Τρύπες"), 356, "Na 'mai pali edo zontanos", listOf("Τρύπες"), 358),
        Case("Τυμβωρύχος", listOf("Δήμητρα Γαλάνη"), 287, "Tynvorychos", listOf("Δήμητρα Γαλάνη"), 286),
        Case("Από πόλη σε πόλη", listOf("Τρύπες"), 195, "Apo poli se poli", listOf("Τρύπες"), 197),
        Case("Vento d'amore (Ο άνεμος της αγάπης)", listOf("Milva"), 163, "Vento d'amore (O anemos tis agapis)", listOf("Milva"), 162),
        Case("دفتر دل", listOf("همایون شجریان"), 0, "Daftare del", listOf("Homayoun Shajarian"), 0),
        Case("فاصله", listOf("Siavash Ghomayshi"), 310, "Faseleh", listOf("Siavash Ghomayshi"), 310),
        Case("خانهء سودا", listOf("همایون شجریان"), 0, "Xâneye sowdâ", listOf("Homayoun Shajarian"), 0),
        Case("نقاب", listOf("Siavash Ghomayshi"), 295, "Neghab", listOf("Siavash Ghomayshi"), 295),
        Case("ઈંધણાં વિણવા ગઈ", listOf("Kaumudi Munshi"), 0, "Indhana Winva Gai", listOf("Kaumudi Munshi"), 0),
        Case("મારું મન મોહી ગયું", listOf("મુકેશ"), 176, "Maru Man Mohi Gayu", listOf("Mukesh"), 176),
        Case("દાન દે વરદાન દે", listOf("મધુસુદન શાસ્ત્રી"), 215, "Daan De Varadan De", listOf("Madhusudan Shastri"), 215),
        Case("હજુ રસભર રાત તો બાકી રહી ગઈ", listOf("વિભા દેસાઈ", "હર્ષદા રાવલ"), 256, "Haju Rasabhar Raat To Baki Rahi Gai", listOf("Vibha Desai", "Harshida Raval"), 256),
        Case("טוב לי", listOf("אריק איינשטיין", "שלום חנוך"), 167, "Tov Li", listOf("Arik Einstein", "Shalom Chanoch"), 167),
        Case("טוב לנו יחד", listOf("מאור כהן"), 268, "Tov Lanu Yahad", listOf("מאור כהן"), 268),
        Case("עץ האלון", listOf("זהבה בן"), 266, "Eets Haalon", listOf("זהבה בן"), 266),
        Case("קח לך אישה", listOf("אריק איינשטיין", "שלום חנוך"), 239, "Kach Lecha Isha", listOf("Arik Einstein", "Shalom Chanoch"), 239),
        Case("गेरुआ", listOf("Pritam", "Arijit Singh", "Antara Mitra"), 346, "Gerua", listOf("Pritam", "Arijit Singh", "Antara Mitra"), 346),
        Case("तेरा बन जाऊँगा", listOf("Akhil Sachdeva", "Tulsi Kumar"), 237, "Tera Ban Jaunga", listOf("Akhil Sachdeva", "Tulsi Kumar"), 237),
        Case("घुंघरू", listOf("Arijit Singh", "Shilpa Rao"), 303, "Ghungroo (From \"WAR\")", listOf("Arijit Singh", "Shilpa Rao"), 303),
        Case("इलाही", listOf("Pritam", "Arijit Singh", "Amitabh Bhattacharya"), 229, "Ilahi", listOf("Pritam", "Arijit Singh", "Amitabh Bhattacharya"), 229),
        Case("勇者の証 〜Brave Eagle Of Apache〜", listOf("東京スカパラダイスオーケストラ"), 264, "Brave Eagle of Apache", listOf("東京スカパラダイスオーケストラ"), 264),
        Case("コイビト", listOf("ROBOTS"), 236, "Koibito", listOf("ROBOTS"), 236),
        Case("ネコだ", listOf("ROBOTS"), 217, "Neko Da", listOf("ROBOTS"), 217),
        Case("いつかどこかで (LIVE MAD MAXX DUB)", listOf("東京スカパラダイスオーケストラ"), 369, "Itsuka Dokoka de (Live Mad Maxx Dub)", listOf("東京スカパラダイスオーケストラ"), 369),
        Case("치유", listOf("Nell"), 315, "Chiyu", listOf("Nell"), 315),
        Case("한계", listOf("Nell"), 225, "Hangye", listOf("Nell"), 225),
        Case("유령의 노래", listOf("Nell"), 262, "Yuryeongui Norae", listOf("Nell"), 262),
        Case("그리움", listOf("Nell"), 282, "Geurium", listOf("Nell"), 282),
        Case("ਬ੍ਰਾਊਨ ਮੁੰਡੇ", listOf("AP Dhillon", "Gurinder Gill", "Shinda Kahlon"), 255, "Brown Munde", listOf("AP Dhillon", "Gurinder Gill", "Shinda Kahlon"), 255),
        Case("ਕਾਲਾ ਚਸ਼ਮਾ", listOf("Amar Arshi", "Badshah", "Neha Kakkar", "Indeep Bakshi"), 188, "Kala Chashma", listOf("Amar Arshi", "Badshah", "Neha Kakkar", "Indeep Bakshi"), 188),
        Case("ਪ੍ਰਾਡਾ", listOf("Jass Manak"), 183, "Prada", listOf("Jass Manak"), 183),
        Case("ਲਾਂਬੜਗਿਨੀ", listOf("The Doorbeen", "Ragini Tandan"), 217, "Lamberghini", listOf("The Doorbeen", "Ragini Tandan"), 217),
        Case("Поэзия", listOf("Аквариум"), 47, "Poeziya", listOf("Аквариум"), 46),
        Case("Победа", listOf("ДДТ"), 331, "Pobeda", listOf("ДДТ"), 330),
        Case("Точка", listOf("Чиж & Cº"), 183, "Tochka", listOf("Чиж & Co"), 183),
        Case("Дверь в лето (II)", listOf("Чиж & Cº"), 126, "Dver' v leto 2", listOf("Чиж & Co"), 126),
        Case("உயிரே", listOf("Hariharan", "K. S. Chithra"), 435, "Uyire Uyire", listOf("Hariharan", "K. S. Chithra"), 435),
        Case("கண்ணான கண்ணே", listOf("Sid Sriram"), 270, "Kannaana Kanney", listOf("Sid Sriram"), 270),
        Case("ரவுடி பேபி", listOf("Dhanush", "Dhee", "Yuvan Shankar Raja"), 282, "Rowdy Baby", listOf("Dhanush", "Dhee", "Yuvan Shankar Raja"), 282),
        Case("முன்பே வா", listOf("Naresh Iyer", "Shreya Ghoshal"), 358, "Munbe Vaa", listOf("Naresh Iyer", "Shreya Ghoshal"), 358),
        Case("నాటు నాటు", listOf("Rahul Sipligunj", "Kaala Bhairava"), 215, "Naatu Naatu", listOf("Rahul Sipligunj", "Kaala Bhairava"), 215),
        Case("ఇంకేం ఇంకేం కావాలే", listOf("Sid Sriram"), 267, "Inkem Inkem Inkem Kaavaale", listOf("Sid Sriram"), 267),
        Case("సామజవరగమన", listOf("Sid Sriram"), 220, "Samajavaragamana", listOf("Sid Sriram"), 220),
        Case("श्रीवल्ली", listOf("जावेद अली"), 225, "Srivalli", listOf("Javed Ali"), 225),
        Case("จะเปลี่ยนเป็นเพื่อน", listOf("Nicole Theriault"), 191, "Cha plian pen phuean", listOf("Nicole Theriault"), 191),
        Case("รักติดปีก", listOf("Job: บรรจบ"), 220, "Ruk Tit Peuk", listOf("Job: บรรจบ"), 220),
        Case("ขังใจ", listOf("Nicole Theriault"), 225, "Khang chai", listOf("Nicole Theriault"), 225),
        Case("ครึ่งควบลูก", listOf("Job: บรรจบ"), 183, "Krueng Kuab Look", listOf("Job: บรรจบ"), 183),
        Case("Ізнов", listOf("Воплі Відоплясова"), 165, "Iznov", listOf("Воплі Відоплясова"), 165),
        Case("Без назви", listOf("Світлана Няньо"), 320, "Bez nazwy", listOf("Svitlana Nianio"), 320),
        Case("Море", listOf("Воплі Відоплясова"), 295, "More", listOf("Воплі Відоплясова"), 295),
        Case("Світ", listOf("Воплі Відоплясова"), 228, "Svit", listOf("Воплі Відоплясова"), 229),
        Case("線上遊戲", listOf("黃義達"), 233, "Xian Shang You Xi", listOf("黃義達"), 233),
        Case("月球上的人 (粵)", listOf("陳奕迅"), 254, "月球上的人 (粤)", listOf("陳奕迅"), 254),
        Case("你讓我又哭又笑", listOf("鄭中基"), 260, "你让我又哭又笑", listOf("郑中基"), 260),
        Case("吵架實錄", listOf("吳克群"), 22, "Chao Jia Shi Lu", listOf("吳克群"), 22),
    )

    private val different = listOf(
        Case("長城", listOf("Beyond"), 291, "Insufferably Arrogant", listOf("Beyond"), 291),
        Case("Птиця‐охітник", listOf("Світлана Няньо"), 212, "Pops", listOf("Svitlana Nianio"), 212),
        Case("지금처럼", listOf("동방신기"), 261, "Charles Chaplin", listOf("TVXQ!"), 261),
        Case("ダンス・オブ・エタニティ", listOf("Li-sa-X"), 380, "Good Day", listOf("Li-sa-X"), 380),
        Case("Біля лісу", listOf("Світлана Няньо"), 345, "Muzika Dika", listOf("Svitlana Nianio"), 345),
        Case("別讓我心疼", listOf("鄭中基"), 269, "月球上的人 (粤)", listOf("郑中基"), 269),
        Case("ময়ূরী আকাশ", listOf("ফিডব্যাক"), 289, "Shwadesh", listOf("Feedback"), 289),
        Case("నీలి నీలి ఆకాశం", listOf("Sid Sriram", "Sunitha"), 284, "Srivalli", listOf("Sid Sriram", "Sunitha"), 284),
        Case("सरीवर सर", listOf("संदीप खरे"), 253, "Illu Illu Pillu", listOf("Sandeep Khare"), 253),
        Case("愛是一本書", listOf("陳奕迅"), 264, "Deng Bian San Jiao", listOf("陳奕迅"), 264),
        Case("হৃদয়হীনা", listOf("Miles"), 355, "Jibon Jala", listOf("MILES"), 355),
        Case("পালকী", listOf("ফিডব্যাক"), 241, "Kotokaal Khujbo Tomay", listOf("Feedback"), 241),
        Case("Колись дівчино мила", listOf("Квітка Цісик"), 121, "Bez nazwy", listOf("Квітка Цісик"), 121),
        Case("এক মুঠো রোদ্দুর", listOf("Balam"), 322, "Meghla Oviman", listOf("Balam"), 322),
        Case("مقدمة", listOf("ريس بيك"), 0, "Hayati", listOf("ريس بيك"), 0),
        Case("Isn’t it сон", listOf("Океан Ельзи"), 220, "Сьогодні", listOf("Okean Elzy"), 220),
    )

    @Test fun acceptsSameRecordingAcrossScripts() = same.forEach {
        val v = RecordingMatch.score(it.t, it.a, it.d, it.ct, it.ca, it.cd)
        assertTrue(v.accepted, "${it.t} vs ${it.ct}: ${v.reason}")
    }

    @Test fun rejectsOtherSongsAcrossScripts() = different.forEach {
        val v = RecordingMatch.score(it.t, it.a, it.d, it.ct, it.ca, it.cd)
        assertTrue(!v.accepted, "${it.t} vs ${it.ct}: ${v.reason}")
    }

    @Test fun guardsFoundBySpike() {
        RecordingMatch.score("Друг ч.1", listOf("Чиж & Co"), 200, "Друг ч.2", listOf("Чиж & Co"), 200).let { assertTrue(!it.accepted, "Друг ч.1: ${it.reason}") }
        RecordingMatch.score("Part 1", listOf("A"), 200, "Part 2", listOf("A"), 200).let { assertTrue(!it.accepted, "Part 1: ${it.reason}") }
        RecordingMatch.score("듣고싶어 (inst.)", listOf("DIA"), 200, "SHE (inst)", listOf("DIA"), 200).let { assertTrue(!it.accepted, "듣고싶어 (inst.): ${it.reason}") }
        RecordingMatch.score("ボディ・ムーヴズ (ライヴ)", listOf("X"), 200, "Body Moves", listOf("X"), 200).let { assertTrue(!it.accepted, "ボディ・ムーヴズ (ライヴ): ${it.reason}") }
        // Latin-only scoring is unchanged by the phonetic tier.
        RecordingMatch.score("Masaal", listOf("A"), 200, "Mosul", listOf("A"), 200).let { assertTrue(!it.accepted, "Masaal: ${it.reason}") }
    }
}
