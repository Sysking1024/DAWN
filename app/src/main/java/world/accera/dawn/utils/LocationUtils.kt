package world.accera.dawn.utils

import com.amap.api.location.AMapLocation

fun getLocationTypeString(type: Int): String {
    return when (type) {
        AMapLocation.LOCATION_TYPE_GPS -> "GPS"
        AMapLocation.LOCATION_TYPE_AMAP -> "高德网络定位"
        AMapLocation.LOCATION_TYPE_OFFLINE -> "离线定位"
        AMapLocation.LOCATION_TYPE_CELL -> "基站定位"
        AMapLocation.LOCATION_TYPE_WIFI -> "WIFI定位"
        AMapLocation.LOCATION_TYPE_FIX_CACHE -> "缓存定位"
        AMapLocation.LOCATION_TYPE_SAME_REQ -> "重复请求在缓存中命中"
        else -> "未知($type)"
    }
}

// ... (在此处复制或引用 getGpsAccuracyStatusString 函数 - 虽然MainScreen未使用，但保留以供参考)
fun getGpsAccuracyStatusString(status: Int): String {
    return when (status) {
        AMapLocation.GPS_ACCURACY_GOOD -> "好 (信号强)"
        AMapLocation.GPS_ACCURACY_BAD -> "差 (信号弱)"
        AMapLocation.GPS_ACCURACY_UNKNOWN -> "未知 (GPS关闭或无法获取信息)"
        else -> "未知状态($status)"
    }
}