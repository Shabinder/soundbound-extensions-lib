package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.models.QueryParams
import `in`.shabinder.soundbound.models.SongModel

abstract class QueryableProvider<TrackEntity, Config : ProviderConfiguration>(
    dependencies: Dependencies
) : Provider<Config>(dependencies) {

    /*
    * Search this SOURCE for matches based on QueryParams and return found matches.
    *  - will return empty list if no matches found
    * */
    abstract suspend fun search(queryParams: QueryParams): List<TrackEntity>

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
    abstract fun TrackEntity.toSongModel(): SongModel
}