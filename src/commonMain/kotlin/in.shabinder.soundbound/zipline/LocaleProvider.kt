package `in`.shabinder.soundbound.zipline

interface LocaleProvider {
    fun getDefaultLocaleCountry(): String
    fun getDefaultLocaleLanguageTag(): String
}