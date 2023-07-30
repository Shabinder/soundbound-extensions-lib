package `in`.shabinder.soundbound.providers.catalog

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.ChartListingContainer
import `in`.shabinder.soundbound.models.ChartListingModel
import kotlinx.serialization.Serializable

@Immutable
@Serializable
abstract class Catalogue {
    abstract suspend fun getCharts(): List<ChartListingModel>
    abstract suspend fun getChartsForCountry(countryCode: String): List<ChartListingModel>

    abstract suspend fun getChartsScreens(): List<ChartListingContainer>
    abstract suspend fun getChartsScreensForCountry(countryCode: String): List<ChartListingContainer>

    @Serializable
    class CatalogueNotAvailable: Catalogue() {
        override suspend fun getCharts(): List<ChartListingModel> = emptyList()
        override suspend fun getChartsForCountry(countryCode: String): List<ChartListingModel> = emptyList()
        override suspend fun getChartsScreens(): List<ChartListingContainer> = emptyList()
        override suspend fun getChartsScreensForCountry(countryCode: String): List<ChartListingContainer> = emptyList()
    }
}