package world.accera.dawn

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.poisearch.PoiSearch.OnPoiSearchListener

class PoiSearchViewModel(application: Application) : AndroidViewModel(application), OnPoiSearchListener {

    val poiListState = mutableStateListOf<PoiItem>()
    val isLoadingState = mutableStateOf(false)
    val errorMessageState = mutableStateOf<String?>(null)
    val suggestionCitiesState = mutableStateListOf<String>()

    private var poiSearch: PoiSearch? = null
    private var currentPage = 0

    fun searchPoi(keyWord: String, cityCode: String = "") {
        if (keyWord.isBlank()) {
            errorMessageState.value = "搜索关键词不能为空"
            return
        }

        Log.d("PoiSearchViewModel", "searchPoi called with keyword: $keyWord, city: $cityCode")

        isLoadingState.value = true
        errorMessageState = null
        poiListState.clear()
        suggestionCitiesState.clear()
        currentPage = 0

        try {
            val query = PoiSearch.Query(keyWord, "", cityCode)
            query.pageSize = 10
            query.pageNum = currentPage

            poiSearch = PoiSearch(getApplication(), query)
            poiSearch.setOnPoiSearchListener(this)

            poiSearch?.searchPOIAsyn()
            Log.d("PoiSearchViewModel", "searchPOIAsyn called")
        } catch (e: AMapException) {
            Log.e("PoiSearchViewModel", "AMapException during search setup: ${e.errorMessage}")
            errorMessageState.value = "搜索发起失败: ${e.errorMessage}"
            isLoadingState.value = false // 发生异常，结束加载状态
        } catch (e: Exception) {
            Log.e("PoiSearchViewModel", "Exception during search setup: ${e.message}", e)
            errorMessageState.value = "搜索发起时发生未知错误"
            isLoadingState.value = false // 发生异常，结束加载状态
        }
    }

    override fun onPoiSearched(result: PoiResult?, rCode: Int) {
        Log.d("PoiSearchViewModel", "onPoiSearched callback received. rCode: $rCode, Result: ${result?.toString()}")
        isLoadingState.value = false // 无论成功与否，加载状态都应结束

        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result?.pois != null && result.pois.isNotEmpty()) {
                poiListState.addAll(result.pois)
                Log.d("PoiSearchViewModel", "POIs found: ${result.pois.size}")
            } else {
                if (result?.searchSuggestionCitys != null && result.searchSuggestionCitys.isNotEmpty()) {
                    val suggestedCities = result.searchSuggestionCitys.mapNotNull { it.cityName }
                    suggestionCitiesState.addAll(suggestedCities)
                    errorMessageState.value = "当前城市未找到结果，请尝试建议城市: ${suggestedCities.joinToString()}"

                    Log.d("PoiSearchViewModel", "No POIs found, but suggestion cities are available: $suggestedCities")
                } else if (result?.searchSuggestionKeywords != null && result?.searchSuggestionKeywords.isNotEmpty()) {
                    errorMessageState.value = "未找到相关结果，建议关键词: ${result.searchSuggestionKeywords.joinToString()}"
                    Log.d("PoiSearchViewModel", "No POIs found, but suggestion keywords are available: ${result.searchSuggestionKeywords.joinToString()}")
                } else {
                    errorMessageState.value = "未找到相关POI信息"
                    Log.d("PoiSearchViewModel", "No POIs found and no suggestions.")
                }
                }
        } else {
            errorMessageState.value = "搜索失败，错误码: $rCode"
            Log.e("PoiSearchViewModel", "Search failed. rCode: $rCode, ErrorInfo: ${result?.toString()} (Check AMap error codes)")
        }
    }

    override fun onPoiItemSearched(p0: PoiItem?, p1: Int) {
        // 对于关键字搜索，此方法通常不会被调用。
        // 如果你后续要实现通过POI ID精确搜索单个POI的功能，则会在这里处理结果。
        // 例如:
        // isLoadingState.value = false
        // if (rCode == AMapException.CODE_AMAP_SUCCESS) {
        //    if (poiItem != null) {
        //        poiListState.clear() // 清空列表，只显示这一个精确结果
        //        poiListState.add(poiItem)
        //    } else {
        //        errorMessageState.value = "未找到该ID对应的POI"
        //    }
        // } else {
        //    errorMessageState.value = "ID搜索失败，错误码: $rCode"
        // }
        Log.d("PoiSearchViewModel", "onPoiItemSearched callback received. rCode: $rCode")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("PoiSearchViewModel", "onCleared called, cleaning up resources.")
        poiSearch = null
    }
}