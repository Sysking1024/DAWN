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

class PoiSearchViewModel(application: Application) : AndroidViewModel(application), PoiSearch.OnPoiSearchListener {

    companion object {
        private const val TAG = "PoiSearchViewModel"
    }

    val poiListState = mutableStateListOf<PoiItem>()
    val isLoadingState = mutableStateOf(false)
    val errorMessageState = mutableStateOf<String?>(null)
    val suggestionCitiesState = mutableStateListOf<String>() // 虽然UI不显示，但保留接收建议城市

    private var poiSearch: PoiSearch? = null
    // private var currentPage = 0 // 目前Demo只取第一页，可以先注释掉

    // *** 调整 searchPoi 方法，cityCode 可以通过参数传入，而不是从UI输入框获取 ***
    fun searchPoi(keyWord: String, cityCode: String = "") {
        if (keyWord.isBlank()) {
            errorMessageState.value = "搜索关键词不能为空"
            // 停止任何可能的加载状态
            isLoadingState.value = false
            return
        }

        // 如果没有传入 cityCode，高德SDK可能会尝试根据当前网络/GPS位置或IP来搜索
        // 或者在全国范围内搜索，具体行为取决于SDK版本和配置。
        // 对于最小Demo，我们依赖 LocationViewModel 提供的城市信息。
        // 如果 LocationViewModel 还没定位成功，调用 searchPoi 时可能传入空 cityCode。

        Log.d(TAG, "搜索POI关键字是：$keyWord, 使用城市代码是：'${cityCode}'") // 记录传入的城市代码

        isLoadingState.value = true
        errorMessageState.value = null
        poiListState.clear()
        suggestionCitiesState.clear()
        // currentPage = 0 // 重置页码（如果后续实现分页）

        try {
            // 城市代码参数现在来自调用者 (LocationViewModel)
            val query = PoiSearch.Query(keyWord, "", cityCode)
            query.pageSize = 10 // 每页10条
            query.pageNum = 0 // 只取第一页 (页码从0开始)

            // 清理旧的 PoiSearch 实例
            poiSearch?.setOnPoiSearchListener(null) // 先解绑监听器
            poiSearch = null // 置空旧实例

            poiSearch = PoiSearch(getApplication(), query)
            poiSearch?.setOnPoiSearchListener(this)

            poiSearch?.searchPOIAsyn() // 发起异步搜索
            Log.d(TAG, "搜索POI开始异步执行")
        } catch (e: AMapException) {
            Log.e(TAG, "搜索发起失败：${e.errorMessage}", e)
            errorMessageState.value = "搜索发起失败: ${e.errorMessage}"
            isLoadingState.value = false // 发生异常，结束加载状态
        } catch (e: Exception) {
            Log.e(TAG, "搜索发起时发生未知错误：${e.message}", e)
            errorMessageState.value = "搜索发起时发生未知错误"
            isLoadingState.value = false // 发生异常，结束加载状态
        }
    }

    override fun onPoiSearched(result: PoiResult?, rCode: Int) {
        Log.d(TAG, "onPoiSearched回调执行rCode：$rCode") // result 可能很大，先不打印result对象
        isLoadingState.value = false // 无论成功与否，加载状态都应结束

        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result?.pois != null && result.pois.isNotEmpty()) {
                poiListState.addAll(result.pois)
                Log.d(TAG, "搜索到 ${result.pois.size} 条结果")
                // 搜索成功后，如果之前有错误信息（例如关键词为空），清除它
                errorMessageState.value = null
            } else {
                // 未找到POI结果，检查是否有建议
                val suggestedCitiesList = result?.searchSuggestionCitys
                val suggestionKeywordsList = result?.searchSuggestionKeywords

                if (!suggestedCitiesList.isNullOrEmpty()) {
                    val suggestedCities = suggestedCitiesList.mapNotNull { it.cityName }
                    // 虽然UI不显示城市建议按钮，但可以在错误信息中提示用户
                    errorMessageState.value = "当前城市未找到结果，请尝试在这些城市中搜索: ${suggestedCities.joinToString()}"
                    suggestionCitiesState.clear() // 清空旧的，虽然UI不显示
                    suggestionCitiesState.addAll(suggestedCities) // 保留数据以防后续需要
                    Log.d(TAG, "当前城市未找到结果，但有建议城市：$suggestedCities")
                } else if (!suggestionKeywordsList.isNullOrEmpty()) {
                    errorMessageState.value = "未找到相关结果，建议关键词: ${suggestionKeywordsList.joinToString()}"
                    Log.d(TAG, "未找到相关结果，建议关键词：${suggestionKeywordsList.joinToString()}")
                }
                else {
                    errorMessageState.value = "未找到相关POI信息"
                    Log.d(TAG, "未找到相关POI信息")
                }
            }
        } else {
            // 搜索失败
            errorMessageState.value = "搜索失败，错误码: $rCode"
            Log.e(TAG, "搜索失败，错误码：$rCode")
        }
    }

    override fun onPoiItemSearched(rPoiItem: PoiItem?, rCode: Int) {
        // 对于关键字搜索，此方法通常不会被调用。
        Log.d(TAG, "onPoiItemSearched callback received. rCode: $rCode")
        // 如果未来实现POI ID搜索，则在这里处理结果，并可能更新isLoadingState = false
    }

    override fun onCleared() {
        super.onCleared()
        // 销毁 PoiSearch 实例，释放资源
        poiSearch?.setOnPoiSearchListener(null) // 先解绑监听器
        poiSearch = null
        Log.d(TAG, "PoiSearchViewModel onCleared, PoiSearch 实例已销毁")
    }

    fun getPoiItemById(poiId: String): PoiItem? {
        return poiListState.find { it.poiId == poiId }
    }
}