package world.accera.dawn

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application), AMapLocationListener {

    companion object {
        private const val TAG = "LocationViewModel"
    }

    // 定位客户端对象
    private var locationClient: AMapLocationClient? = null
    // 定位场景参数对象
    private var locationOption: AMapLocationClientOption? = null

    // 定位结果状态，使用 MutableState 在Compose中观察
    val locationResultState = mutableStateOf<AMapLocation?>(null)
    val locationErrorState = mutableStateOf<String?>(null)
    val isLocatingState = mutableStateOf(false)
    val isContinuousModeActive = mutableStateOf(false)


    override fun onLocationChanged(aMapLocation: AMapLocation?) {
        if (aMapLocation != null) {
            if (aMapLocation.errorCode == 0) {
                // 定位成功回调信息，设置相关消息
                locationResultState.value = aMapLocation
                locationErrorState.value = null
                Log.d(TAG, "定位成功!")

                // 如果是单次定位，可以在这里停止定位
                if (locationOption?.isOnceLocation == true) {
                    stopLocation()
                }
            } else {
                // 定位失败时，可通过ErrCode（错误码）信息来确定失败的原因
                val errorInfo = "定位失败, ErrCode：${aMapLocation.errorCode}，errInfo"
                locationErrorState.value = errorInfo
                locationResultState.value = null // 清除旧的成功结果
                Log.e(TAG, "定位失败")
                // 如果是单次定位失败，也应更新状态
                if (locationOption?.isOnceLocation == true) {
                    isLocatingState.value = false
                }
            }
        } else {
            val errorInfo = "amapLocation对象为null"
            locationErrorState.value = errorInfo
            locationResultState.value = null
            Log.e(TAG, errorInfo)
            if (locationOption?.isOnceLocation == true) {
                isLocatingState.value = false
            }
        }
    }

    init {
        // 初始化定位客户端
        try {
            // 此处传入getApplication() appContext，是因为官方文档推荐用getApplicationContext()
            locationClient = AMapLocationClient(this.getApplication<Application>().applicationContext)
            locationClient?.setLocationListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "初始化AMapLocationClient失败", e)
            locationErrorState.value = "初始化定位客户端失败: ${e.message}"
        }
    }


    fun startLocation(isOnce: Boolean = true, needAddress: Boolean = true) {

        if (!hasAllPermissions()) {
            locationErrorState.value = "定位权限不足"
            Log.w(TAG, "定位权限不足，请先授予权限")
            return
        }


        // 关键：如果 client 为 null，重新初始化
        if (locationClient == null) {
            try {
                locationClient = AMapLocationClient(this.getApplication<Application>().applicationContext)
                locationClient?.setLocationListener(this)
                Log.d(TAG, "权限授予后重新初始化 AMapLocationClient")
            } catch (e: Exception) {
                Log.e(TAG, "初始化AMapLocationClient失败", e)
                locationErrorState.value = "初始化定位客户端失败: ${e.message}"
                return
            }
        }

        isLocatingState.value = true
        isContinuousModeActive.value = isOnce
        locationErrorState.value = null // 清除之前的错误
        // 初始化AMapLocationClientOption对象
        locationOption = AMapLocationClientOption().apply {
            // 设置定位模式为高精度模式。
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            // 设置是否获取一次定位结果。
            isOnceLocation = isOnce
            if (isOnce) {
                // 获取最近3s内精度最高的一次定位结果。
                // setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。
                // 如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会。
                isOnceLocationLatest = true
            } else {
                // 设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
                interval = 2000 // 连续定位时间间隔
            }
            // 设置是否返回地址信息（默认返回地址信息）
            isNeedAddress = needAddress
            // isGpsFirst = true
            // 设置是否允许模拟位置,默认为true，允许模拟位置
            // setMockEnable(true) // 通常调试时开启，发布时关闭或移除
            // 单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
            httpTimeOut = 20000
            // 关闭缓存机制，以获取最新定位结果 (根据需求调整)
            isLocationCacheEnable = false
            // 设置定位场景，例如：出行
            // setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Transport)
        }

        //给定位客户端对象设置定位参数
        locationClient?.setLocationOption(locationOption)
        //启动定位
        locationClient?.startLocation()
        Log.d(TAG, "开始定位, isOnce: $isOnce, needAddress: $needAddress")
    }

    private fun hasAllPermissions(): Boolean {
        val context = getApplication<Application>().applicationContext
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.CAMERA
        )
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun stopLocation() {
        locationClient?.stopLocation()
        isLocatingState.value = false
        isContinuousModeActive.value = false
        Log.d(TAG, "停止定位")
    }

    override fun onCleared() {
        super.onCleared()
        locationClient?.onDestroy()
        locationClient = null
        locationOption = null
        Log.d(TAG, "LocationViewModel onCleared, 定位客户端已销毁")
    }

}