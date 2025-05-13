package world.accera.dawn

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.location.AMapLocation
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocationScreen(
    locationViewModel: LocationViewModel = viewModel(),
    onRequestPermissions: (Array<String>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 从ViewModel观察状态
    val locationResult by locationViewModel.locationResultState
    val locationError by locationViewModel.locationErrorState
    val isLocating by locationViewModel.isLocatingState
    val isContinuousModeActive by locationViewModel.isContinuousModeActive

    // 收集权限请求事件
    LaunchedEffect(Unit) { // 使用 Unit 确保只在 Composable 首次进入组合时订阅一次
        locationViewModel.permissionRequestFlow.collect { permissionsToRequest ->
            onRequestPermissions(permissionsToRequest)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp) // 元素间的垂直间距
    ) {
        Text("高德定位SDK Demo", style = MaterialTheme.typography.headlineSmall)

        Button(
            onClick = { locationViewModel.startLocation(isOnce = true) },
            enabled = !isLocating
        ) {
            Text(if (isLocating && locationResult == null) "正在单次定位..." else "开始单次定位")
        }

        Button(
            onClick = { locationViewModel.startLocation(isOnce = false, needAddress = true) },
            enabled = !isLocating || locationResult?.locationType != -1 /*允许在连续定位时再次点击以刷新参数，或显示停止按钮*/
        ) {
            Text(if (isLocating && !isContinuousModeActive) "正在连续定位..." else "开始连续定位 (2s间隔)")
        }


        if (isLocating && !isContinuousModeActive) { // 如果是连续定位模式，则显示停止按钮
            Button(onClick = { locationViewModel.stopLocation() }) {
                Text("停止连续定位")
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        if (isLocating && locationResult == null && locationError == null) {
            CircularProgressIndicator()
            Text("正在获取定位信息...")
        }

        locationError?.let { error ->
            Text("定位错误: $error", color = MaterialTheme.colorScheme.error)
        }

        locationResult?.let { loc ->
            LocationInfoView(location = loc)
        }

        // 检查权限按钮 (用于手动检查和请求)
        Button(onClick = {
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!fineLocationGranted) {
                coroutineScope.launch { // 确保在协程中调用 suspend 方法
                    locationViewModel.permissionRequestFlow.collect{ permissionsArray ->
                        onRequestPermissions(permissionsArray)
                    }
                }
                // 或者直接调用请求
                // onRequestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            } else {
                // Toast.makeText(context, "定位权限已授予", Toast.LENGTH_SHORT).show()
                Log.d("LocationScreen", "定位权限已授予")
            }
            // Manually trigger start after permission check if needed
            locationViewModel.startLocation(isOnce = true)

        }) {
            Text("检查/请求定位权限并定位")
        }
    }
}

@Composable
fun LocationInfoView(location: AMapLocation) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("定位结果:", style = MaterialTheme.typography.titleMedium)
            InfoRow("时间:", sdf.format(Date(location.time)))
            InfoRow("来源:", getLocationTypeString(location.locationType))
            InfoRow("经度:", location.longitude.toString())
            InfoRow("纬度:", location.latitude.toString())
            InfoRow("精度(米):", location.accuracy.toString())
            if (location.speed > 0) InfoRow("速度(米/秒):", location.speed.toString())
            if (location.bearing > 0) InfoRow("方向(度):", location.bearing.toString())
            if (location.altitude > 0) InfoRow("海拔(米):", location.altitude.toString())

            if (location.address.isNotBlank()) {
                Text("地址信息:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                InfoRow("国家:", location.country)
                InfoRow("省份:", location.province)
                InfoRow("城市:", location.city)
                InfoRow("城市编码:", location.cityCode)
                InfoRow("区:", location.district)
                InfoRow("街道:", location.street)
                InfoRow("门牌号:", location.streetNum)
                InfoRow("区域编码:", location.adCode)
                InfoRow("AOI名称:", location.aoiName)
                InfoRow("POI名称:", location.poiName)
                InfoRow("完整地址:", location.address)
            }
            InfoRow("GPS状态:", getGpsAccuracyStatusString(location.gpsAccuracyStatus))
            InfoRow("错误码:", location.errorCode.toString())
            if(location.errorCode != 0) InfoRow("错误信息:", location.errorInfo)
            // InfoRow("详情:", location.locationDetail) // 用于调试
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(100.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

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

fun getGpsAccuracyStatusString(status: Int): String {
    return when (status) {
        AMapLocation.GPS_ACCURACY_GOOD -> "好 (信号强)"
        AMapLocation.GPS_ACCURACY_BAD -> "差 (信号弱)"
        AMapLocation.GPS_ACCURACY_UNKNOWN -> "未知 (GPS关闭或无法获取信息)"
        else -> "未知状态($status)"
    }
}
// 在你的主题文件中定义 YourAppNameTheme
// 例如在 ui/theme/Theme.kt
// @Composable
// fun YourAppNameTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    content: @Composable () -> Unit
// ) {
//    val colorScheme = if (darkTheme) {
//        DarkColorScheme
//    } else {
//        LightColorScheme
//    }
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography, // 确保你有 Typography 定义
//        content = content
//    )
// }