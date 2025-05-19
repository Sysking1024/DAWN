package world.accera.dawn

// *** 导入位于 data 包中的数据结构 ***

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.amap.api.navi.AMapNavi
import com.amap.api.navi.AMapNaviListener
import com.amap.api.navi.AMapNaviViewListener
import com.amap.api.navi.enums.PathPlanningStrategy
import com.amap.api.navi.model.AMapCalcRouteResult
import com.amap.api.navi.model.AMapLaneInfo
import com.amap.api.navi.model.AMapModelCross
import com.amap.api.navi.model.AMapNaviCameraInfo
import com.amap.api.navi.model.AMapNaviCross
import com.amap.api.navi.model.AMapNaviLocation
import com.amap.api.navi.model.AMapNaviPath
import com.amap.api.navi.model.AMapNaviRouteNotifyData
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo
import com.amap.api.navi.model.AMapServiceAreaInfo
import com.amap.api.navi.model.AMapTravelInfo
import com.amap.api.navi.model.AimLessModeCongestionInfo
import com.amap.api.navi.model.AimLessModeStat
import com.amap.api.navi.model.NaviInfo
import com.amap.api.navi.model.NaviLatLng
import world.accera.dawn.data.RouteOptionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class RoutePlanViewModel(application: Application) : AndroidViewModel(application),
    AMapNaviListener, AMapNaviViewListener {

    companion object {
        private const val TAG = "RoutePlanViewModel"
        // 交通方式索引，与 RoutePlanScreen 中的 Tab 对应
        private const val TRANSPORT_MODE_WALK = 0
        private const val TRANSPORT_MODE_BUS = 1
        private const val TRANSPORT_MODE_DRIVING = 2
        private const val TRANSPORT_MODE_TAXI = 3
    }

    // --- ViewModel 暴露给 UI 的状态 ---
    private val _allRouteDataState = mutableStateMapOf<Int, List<RouteOptionSummary>>()
    val allRouteDataState: State<Map<Int, List<RouteOptionSummary>>> = derivedStateOf {
        // 将可变的 SnapshotStateMap 转换为一个只读的 Map 并在 State 中暴露
        _allRouteDataState.toMap()
    }

    private val _isLoadingState = mutableStateOf(false)
    val isLoadingState: State<Boolean> = _isLoadingState

    private val _errorMessageState = mutableStateOf<String?>(null)
    val errorMessageState: State<String?> = _errorMessageState

    // --- ViewModel 内部状态 ---
    private var aMapNavi: AMapNavi? = null
    // private var routeSearch: RouteSearch? = null // *** 移除 RouteSearch 实例 ***

    private var currentOrigin: NaviLatLng? = null
    private var currentDestination: NaviLatLng? = null
    private var currentDestinationName: String? = null // 存储终点名称用于UI

    private var lastCalculatedModeIndex: Int = -1 // 记录最后一次尝试规划的模式

    // --- 初始化 ---
    init {
        Log.d(TAG, "RoutePlanViewModel init")
        try {
            // 获取 AMapNavi 单例实例
            // 使用 Application context 避免内存泄漏
            aMapNavi = AMapNavi.getInstance(getApplication<Application>().applicationContext)
            // 添加监听器
            aMapNavi?.addAMapNaviListener(this)
            // 骑行/步行兼容性设置
            // TODO: 需要确认是否仅在骑行/步行场景需要此设置，或者是否在所有场景都需要？
            // 暂时只在步行和骑行算路时设置，或者在 init 中设置一次 setIsNaviTravelView(true) 然后在驾车算路时设置 setIsNaviTravelView(false)?
            // 文档说“骑步行仍是历史实现，需要使用该接口进行历史兼容状态判断，驾车场景入参为false，骑步行场景入参为true”
            // 方案：在init设置一次 true，在 planRoute 内部根据模式再次设置
            aMapNavi?.isNaviTravelView = true
            aMapNavi?.setUseInnerVoice(true, false)

            Log.d(TAG, "AMapNavi initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AMapNavi", e)
            _errorMessageState.value = "导航服务初始化失败: ${e.message}"
            _isLoadingState.value = false
        }
    }

    // --- 规划路径方法 ---
    /**
     * 发起路径规划请求
     * @param originLat 起点纬度
     * @param originLon 起点经度
     * @param destLat 终点纬度
     * @param destLon 终点经度
     * @param destName 终点名称
     * @param transportModeIndex 交通方式索引 (0:步行, 1:公交, 2:驾车, 3:打车)
     */
    fun planRoute(originLat: Double, originLon: Double, destLat: Double, destLon: Double, destName: String, transportModeIndex: Int) {
        if (aMapNavi == null) {
            _errorMessageState.value = "导航服务未初始化"
            Log.e(TAG, "AMapNavi is null, cannot plan route.")
            _isLoadingState.value = false
            return
        }

        // 记录当前的起终点和模式，方便回调中处理
        currentOrigin = NaviLatLng(originLat, originLon)
        currentDestination = NaviLatLng(destLat, destLon)
        currentDestinationName = destName // 存储终点名称用于UI显示
        lastCalculatedModeIndex = transportModeIndex

        Log.d(TAG, "开始规划路径: Mode=${getTransportModeName(transportModeIndex)}, Origin=(${originLat}, ${originLon}), Dest=(${destLat}, ${destLon}), DestName=${destName}")

        _isLoadingState.value = true
        _errorMessageState.value = null
        // 清空当前模式的旧结果，其他模式结果保留
        _allRouteDataState[transportModeIndex] = emptyList()


        val startNaviLatLng = currentOrigin
        val endNaviLatLng = currentDestination

        if (startNaviLatLng == null || endNaviLatLng == null) {
            _errorMessageState.value = "起终点信息无效，无法发起规划"
            _isLoadingState.value = false
            Log.e(TAG, "Start or End NaviLatLng is null")
            return
        }


        when (transportModeIndex) {
            TRANSPORT_MODE_WALK -> {
                // 步行规划 (使用 AMapNavi)
                aMapNavi?.isNaviTravelView = true // 步行设置兼容模式
                val success = aMapNavi!!.calculateWalkRoute(startNaviLatLng, endNaviLatLng)
                if (!success) {
                    _errorMessageState.value = "发起步行规划请求失败"
                    _isLoadingState.value = false
                    Log.e(TAG, "aMapNavi.calculateWalkRoute returned false")
                } else {
                    Log.d(TAG, "步行规划请求已发起")
                }
            }
            TRANSPORT_MODE_BUS -> {
                // 公交规划 (占位符) - 需要使用搜索SDK，且 TransitRouteQuery 可能缺失
                _errorMessageState.value = "${getTransportModeName(transportModeIndex)}规划功能待开发"
                _isLoadingState.value = false
                Log.w(TAG, "公交规划功能待开发")
                // 清空当前模式的方案列表（在函数开头已经做了）
            }
            TRANSPORT_MODE_DRIVING -> {
                // 驾车规划 (使用 AMapNavi)
                aMapNavi?.isNaviTravelView = false // 驾车关闭兼容模式
                val startList = listOf(startNaviLatLng)
                val endList = listOf(endNaviLatLng)
                // 使用经纬度列表算驾车路径，单路径策略
                val success = aMapNavi!!.calculateDriveRoute(startList, endList, null, PathPlanningStrategy.NO_STRATEGY)

                if (!success) {
                    _errorMessageState.value = "发起驾车规划请求失败"
                    _isLoadingState.value = false
                    Log.e(TAG, "aMapNavi.calculateDriveRoute returned false")
                } else {
                    Log.d(TAG, "驾车规划请求已发起")
                }
            }
            TRANSPORT_MODE_TAXI -> {
                // 打车规划 (占位符) - 实际可能需要调用第三方打车服务
                _errorMessageState.value = "${getTransportModeName(transportModeIndex)}规划功能待开发"
                _isLoadingState.value = false
                Log.w(TAG, "打车规划功能待开发")
                // 清空当前模式的方案列表（在函数开头已经做了）
            }
            else -> {
                _errorMessageState.value = "未知交通方式: ${getTransportModeName(transportModeIndex)}"
                _isLoadingState.value = false
            }
        }
    }

    // --- 发起导航方法 ---
    /**
     * 发起实时导航
     */
    private fun startNavigation() {
        if (aMapNavi == null) {
            _errorMessageState.value = "导航服务未初始化"
            Log.e(TAG, "AMapNavi is null, cannot start navigation.")
            // Optional: Update a state to indicate navigation failed to start
            return
        }

        try {
            // 发起导航
            // NaviType.GPS corresponds to real-time navigation (int 1)
            val success = aMapNavi!!.startNavi(2)

            if (success) {
                Log.d(TAG, "导航已成功发起")
                // 导航成功发起后，通常应用会切换到高德导航的内置UI
                // 这个Demo目前只实现启动SDK引擎，不包含切换到导航UI的逻辑
                // TODO: 在启动导航成功后，可能需要通知 UI 切换到导航界面 (例如，通过 SharedFlow)
            } else {
                _errorMessageState.value = "发起导航失败"
                Log.e(TAG, "aMapNavi.startNaviWithPath returned false for")
                // Optional: Update a state to indicate navigation failed to start
            }
        } catch (e: Exception) {
            _errorMessageState.value = "发起导航时发生错误: ${e.message}"
            Log.e(TAG, "Error starting navigation with NaviType ", e)
            // Optional: Update a state
        }
    }

    // --- UI 回调：用户选中方案并点击出发/轿车按钮 ---
    /**
     * 处理用户在 UI 上选中某个方案并点击了出发/轿车按钮
     * 这个方法由 RoutePlanScreen 中的卡片按钮点击回调触发
     */
    fun onRouteOptionSelectedForNavigation() {
        Log.d(TAG, "User selected path with ID:  for navigation. Calling startNavigation.")
        // 调用内部方法发起实时导航 (NaviType.GPS)
        startNavigation()
    }

    // --- 辅助方法：将 SDK 返回的路线数据转换为 UI 需要的 RouteOptionSummary ---
    /**
     * 将 AMapNaviPath 转换为 RouteOptionSummary (主要用于步行/骑行/驾车由 AMapNavi 返回的情况)
     */
    private fun convertNaviPathToSummary(path: AMapNaviPath, index: Int, modeIndex: Int): RouteOptionSummary {
        val id = path.hashCode().toString() // 使用哈希码作为方案ID
        val title = "${getTransportModeName(modeIndex)}方案 ${index + 1}" // 方案标题

        // 提取并格式化距离和时间
        val distanceText = formatDistance(path.allLength) // 距离 (米)
        val durationText = formatDuration(path.allTime) // 时间 (秒)

        // 计算预计到达时间 (当前时间 + 预计时间)
        val arrivalTimeText = calculateArrivalTime(path.allTime)

        // 根据模式生成第一行文本 (简化处理，从路径数据提取复杂，先基于模式简单描述)
        val firstLineText = getFirstLineText(path, modeIndex)

        // 根据模式生成按钮文本
        val buttonText = if (modeIndex == TRANSPORT_MODE_TAXI) "轿车" else "出发" // 打车模式已是占位，但保留逻辑

        // 组合距离、时间、到达时间为一行文本
        val timeInfoText = "$distanceText | $durationText | $arrivalTimeText 到达"

        return RouteOptionSummary(
            id = id,
            title = title,
            firstLineText = firstLineText,
            timeInfoText = timeInfoText,
            buttonText = buttonText,
            path = path // 存储原始路径对象，方便后续使用 (如开始导航)
        )
    }

    /**
     * 根据模式和路径数据生成第一行文本 (简化实现)
     */
    private fun getFirstLineText(path: AMapNaviPath, modeIndex: Int): String {
        return when (modeIndex) {
            TRANSPORT_MODE_WALK -> {
                // 步行：可以尝试统计路口数量或其他简单信息
                "步行路线概要" // 如果需要更详细的概要，可能需要遍历 path.getSteps() 或 path.getNaviGuideList()
            }
            TRANSPORT_MODE_DRIVING -> {
                // 驾车：可以尝试提取红绿灯数量，路况信息等
                val trafficLightCount = path.trafficLightCount // 获取红绿灯总数
            val tollCost = path.tollCost // 获取过路费

                var text = "$trafficLightCount 个红绿灯"
                if (tollCost > 0) {
                    text += " | 预估 ¥${tollCost} 过路费"
                }
                if (text.isEmpty() || text == "0 个红绿灯") {
                    text = "驾车路线概要" // 提供一个通用概要
                }// 如果只有0个红绿灯且没有过路费
                text
            }
            TRANSPORT_MODE_BUS -> "公交路线概要" // 占位，实际需要解析 TransitRouteResult
            TRANSPORT_MODE_TAXI -> "打车信息概要" // 占位，实际需要第三方数据
            else -> "路线概要"
        }
    }


    /**
     * 根据模式索引获取交通方式名称
     */
    private fun getTransportModeName(modeIndex: Int): String {
        return when (modeIndex) {
            TRANSPORT_MODE_WALK -> "步行"
            TRANSPORT_MODE_BUS -> "公交"
            TRANSPORT_MODE_DRIVING -> "驾车"
            TRANSPORT_MODE_TAXI -> "打车"
            else -> "未知"
        }
    }

    // --- 距离格式化辅助函数 (米 -> km/m) ---
    private fun formatDistance(meters: Int): String {
        return if (meters < 1000) {
            "$meters 米"
        } else {
            String.format(Locale.getDefault(), "%.1f 公里", meters / 1000.0)
        }
    }

    // --- 时间格式化辅助函数 (秒 -> 分/小时) ---
    private fun formatDuration(seconds: Int): String {
        return if (seconds < 60) {
            "$seconds 秒"
        } else if (seconds < 3600) {
            "${seconds / 60} 分钟"
        } else {
            String.format(Locale.getDefault(), "%d 小时 %d 分钟", seconds / 3600, (seconds % 3600) / 60)
        }
    }

    // --- 计算预计到达时间 (当前时间 + 预计时间) ---
    private fun calculateArrivalTime(durationSeconds: Int): String {
        val currentTimeMillis = System.currentTimeMillis()
        val arrivalTimeMillis = currentTimeMillis + durationSeconds * 1000L
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(Date(arrivalTimeMillis))
    }

    // --- AMapNaviListener 回调实现 ---

    override fun onInitNaviSuccess() {
        Log.d(TAG, "onInitNaviSuccess")
        // 初始化成功，可以在这里发起第一次算路
        // 但更好的做法是在 UI 接收到起终点参数后触发算路
        _errorMessageState.value = null // 初始化成功，清除可能的错误
    }

    override fun onInitNaviFailure() {
        Log.e(TAG, "onInitNaviFailure")
        _errorMessageState.value = "导航服务初始化失败"
        _isLoadingState.value = false
    }

    override fun onCalculateRouteSuccess(routeResult: AMapCalcRouteResult) {
        Log.d(TAG, "onCalculateRouteSuccess: RouteId = ${routeResult.routeid?.joinToString()}, ErrorCode = ${routeResult.errorCode}")
        _isLoadingState.value = false
        _errorMessageState.value = null // 算路成功，清除错误信息

        // 获取规划出的所有路径
        val naviPaths = aMapNavi?.naviPaths

        // 确保 naviPaths 不为空且包含路径
        if (!naviPaths.isNullOrEmpty()) {
            Log.d(TAG, "Obtained ${naviPaths.size} paths from getNaviPaths()")
            // 提取并转换路径为 RouteOptionSummary 列表
            val routeSummaries = naviPaths.values.mapIndexed { index, path ->
                // 使用记录的最后一次尝试规划的模式来转换
                convertNaviPathToSummary(path, index, lastCalculatedModeIndex)
            }

            // 更新对应模式的状态
            _allRouteDataState[lastCalculatedModeIndex] = routeSummaries
            aMapNavi?.setTravelInfo(AMapTravelInfo(TRANSPORT_MODE_WALK))

            Log.d(TAG, "成功规划 ${routeSummaries.size} 条 ${getTransportModeName(lastCalculatedModeIndex)} 路线 (AMapNavi)")
        } else {
            _errorMessageState.value = "未找到 ${getTransportModeName(lastCalculatedModeIndex)} 路线方案 (AMapNavi)"
            Log.w(TAG, "onCalculateRouteSuccess: naviPaths is null or empty")
            // 清空当前模式的方案列表
            _allRouteDataState[lastCalculatedModeIndex] = emptyList()
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun notifyParallelRoad(p0: Int) {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun OnUpdateTrafficFacility(p0: Array<out AMapNaviTrafficFacilityInfo>?) {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun OnUpdateTrafficFacility(p0: AMapNaviTrafficFacilityInfo?) {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java")
    override fun onCalculateRouteFailure(errorID: Int) {
        Log.e(TAG, "onCalculateRouteFailure: errorID = $errorID")
        _isLoadingState.value = false
        // 根据 errorID 获取更详细的错误信息（高德SDK有错误码对照表）
        val errorMsg = "规划失败: ${getTransportModeName(lastCalculatedModeIndex)} (错误码: $errorID)" // TODO: 映射错误码到用户友好文本
        _errorMessageState.value = errorMsg

        // 清空当前模式的方案列表
        _allRouteDataState[lastCalculatedModeIndex] = emptyList()

        // 如果是由于网络等瞬时错误，可以考虑在此重试
    }

    override fun onCalculateRouteFailure(p0: AMapCalcRouteResult?) {
        TODO("Not yet implemented")
    }

    override fun onReCalculateRouteForYaw() {
        TODO("Not yet implemented")
    }

    override fun onReCalculateRouteForTrafficJam() {
        TODO("Not yet implemented")
    }

    override fun onArrivedWayPoint(p0: Int) {
        TODO("Not yet implemented")
    }


    // --- 其他 AMapNaviListener 回调 (只需简单实现或日志) ---
    // AMapNaviListener 接口有很多方法，需要全部实现，这里列出部分常用和所有接口
    // 注意：高德SDK版本不同，接口可能略有差异，确保实现所有方法
    override fun onStartNavi(p0: Int) { Log.d(TAG, "onStartNavi") }
    override fun onTrafficStatusUpdate() { /*Log.d(TAG, "onTrafficStatusUpdate")*/ } // 频率较高，可能不需要日志
    override fun onLocationChange(p0: AMapNaviLocation?) { /*Log.d(TAG, "onLocationChange")*/ } // 频率较高
    override fun onGetNavigationText(p0: Int, p1: String?) { /*Log.d(TAG, "onGetNavigationText")*/ }
    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun onGetNavigationText(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun onEndEmulatorNavi() {
        TODO("Not yet implemented")
    }

    override fun onArriveDestination() {
        TODO("Not yet implemented")
    }

    override fun onGpsSignalWeak(p0: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onGpsOpenStatus(p0: Boolean) { Log.d(TAG, "onGpsOpenStatus: $p0") }
    override fun onNaviInfoUpdate(p0: NaviInfo?) {
        TODO("Not yet implemented")
    }

    override fun updateCameraInfo(p0: Array<out AMapNaviCameraInfo>?) {
        TODO("Not yet implemented")
    }

    override fun updateIntervalCameraInfo(
        p0: AMapNaviCameraInfo?,
        p1: AMapNaviCameraInfo?,
        p2: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun onServiceAreaUpdate(p0: Array<out AMapServiceAreaInfo>?) { /*Log.d(TAG, "onServiceAreaUpdate")*/ }
    override fun showCross(p0: AMapNaviCross?) {
        TODO("Not yet implemented")
    }

    override fun hideCross() {
        TODO("Not yet implemented")
    }

    override fun showModeCross(p0: AMapModelCross?) {
        TODO("Not yet implemented")
    }

    override fun hideModeCross() {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun showLaneInfo(p0: Array<out AMapLaneInfo>?, p1: ByteArray?, p2: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun showLaneInfo(p0: AMapLaneInfo?) {
        TODO("Not yet implemented")
    }

    override fun hideLaneInfo() {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun onCalculateRouteSuccess(p0: IntArray?) {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java")
    override fun updateAimlessModeStatistics(p0: AimLessModeStat?) { /*Log.d(TAG, "updateAimlessModeStatistics")*/ }
    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun updateAimlessModeCongestionInfo(p0: AimLessModeCongestionInfo?) {
        TODO("Not yet implemented")
    }

    override fun onPlayRing(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onNaviRouteNotify(p0: AMapNaviRouteNotifyData?) {
        TODO("Not yet implemented")
    }

    // --- ViewModel 销毁时释放资源 ---
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "RoutePlanViewModel onCleared, destroying AMapNavi")
        // 移除监听器
        aMapNavi?.removeAMapNaviListener(this)
        // 销毁实例 - AMapNavi 是单例，使用静态方法销毁
        AMapNavi.destroy()
        aMapNavi = null
    }

    override fun onNaviSetting() {
        TODO("Not yet implemented")
    }

    override fun onNaviCancel() {
        TODO("Not yet implemented")
    }

    override fun onNaviBackClick(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onNaviMapMode(p0: Int) {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun onNaviTurnClick() {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun onNextRoadClick() {
        TODO("Not yet implemented")
    }

    override fun onScanViewButtonClick() {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"Not yet implemented\")"))
    override fun onLockMap(p0: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onNaviViewLoaded() {
        TODO("Not yet implemented")
    }

    override fun onMapTypeChanged(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onNaviViewShowMode(p0: Int) {
        TODO("Not yet implemented")
    }
}