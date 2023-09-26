package `in`.shabinder.soundbound.providers.catalog

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.ChartListingContainer
import `in`.shabinder.soundbound.models.ChartListingModel

@Immutable
interface Catalogue {
    suspend fun getCharts(): List<ChartListingModel>
    suspend fun getChartsForCountry(countryCode: String): List<ChartListingModel> = emptyList()

    suspend fun getChartsScreens(): List<ChartListingContainer> = emptyList()
    suspend fun getChartsScreensForCountry(countryCode: String): List<ChartListingContainer> =
        emptyList()

    @Immutable
    interface CatalogueNotAvailable : Catalogue {
        override suspend fun getCharts(): List<ChartListingModel> = emptyList()
        override suspend fun getChartsForCountry(countryCode: String): List<ChartListingModel> =
            emptyList()

        override suspend fun getChartsScreens(): List<ChartListingContainer> = emptyList()
        override suspend fun getChartsScreensForCountry(countryCode: String): List<ChartListingContainer> =
            emptyList()
    }
}