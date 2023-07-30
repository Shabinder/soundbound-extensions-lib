package `in`.shabinder.soundbound.providers

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.QueryParams
import `in`.shabinder.soundbound.models.SearchItem
import `in`.shabinder.soundbound.models.SongModel
import kotlinx.serialization.Serializable

interface QueryableProvider<TrackEntity, Config : ProviderConfiguration> : Provider<Config>,
    ZiplineService {

    /*
    * Search this SOURCE for matches based on QueryParams and return found matches.
    *  - will return empty list if no matches found
    * */
    abstract suspend fun search(queryParams: QueryParams): List<TrackEntity>

    open val isISRCSupported: Boolean get() = false

    open suspend fun searchSongModels(queryParams: QueryParams): List<SongModel> {
        return search(queryParams).map { toSongModel(it) }
    }

    // this is without auto-completions
    abstract suspend fun searchItems(queryParams: QueryParams): List<SearchItem>

    // only auto-completions
    open suspend fun searchSuggestionItems(queryParams: QueryParams): List<SearchItem> = emptyList()

    /*
    * Return The Best Match from Provided TrackEntity
    *   Will throw `ProviderException.NoMatchFound` if queryResults is empty
    * */
    abstract suspend fun sortByBestMatch(
        queryResults: List<TrackEntity>,
        queryParams: QueryParams
    ): TrackEntity

    /*
    * Function to map TrackEntity -> TrackDetails.
    * */
    abstract fun toSongModel(entity: TrackEntity): SongModel
}