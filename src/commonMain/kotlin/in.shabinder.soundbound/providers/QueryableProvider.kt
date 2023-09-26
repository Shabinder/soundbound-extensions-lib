package `in`.shabinder.soundbound.providers

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.QueryParams
import `in`.shabinder.soundbound.models.SearchItem
import `in`.shabinder.soundbound.models.SongModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface QueryableProvider : Provider, ZiplineService {

    val isISRCSupported: Boolean get() = false

    suspend fun searchSongModels(queryParams: QueryParams): List<SongModel>

    // this is without auto-completions
    suspend fun searchItems(queryParams: QueryParams): List<SearchItem>
    fun loadItems(queryParams: QueryParams): Flow<List<SearchItem>>

    // only auto-completions
    suspend fun searchSuggestionItems(queryParams: QueryParams): List<SearchItem> = emptyList()
    fun loadSuggestionItems(queryParams: QueryParams): Flow<List<SearchItem>> = flowOf(emptyList())
}