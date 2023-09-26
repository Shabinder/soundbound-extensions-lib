package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

interface LocaleProvider : ZiplineService {
    fun getDefaultLocaleCountry(): String
    fun getDefaultLocaleLanguageTag(): String
}