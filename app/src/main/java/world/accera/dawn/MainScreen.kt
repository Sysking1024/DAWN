package world.accera.dawn

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.location.AMapLocation
import com.amap.api.maps.MapView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    locationViewModel: LocationViewModel = viewModel(),
    // 点击搜索框时的回调，用于触发导航到 POI 搜索界面
    onSearchClick: () -> Unit = {}
) {
    val context = LocalContext.current
    // 观察 LocationViewModel 中的定位结果和状态
    val locationResult by locationViewModel.locationResultState
    val locationError by locationViewModel.locationErrorState
    val isLocating by locationViewModel.isLocatingState

    // 在屏幕首次显示时尝试开始定位（如果权限已授予）
    // 注意：ViewModel内部的权限检查和请求逻辑会在startLocation中触发
    LaunchedEffect(Unit) {
        // 如果ViewModel不是正在定位且没有错误，可以尝试开始定位
        // ViewModel内部会处理权限检查和请求逻辑
        if (!isLocating && locationResult == null && locationError == null) {
            locationViewModel.startLocation(isOnce = true, needAddress = true)
        }
    }

    // 替换原来的中间区域部分
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 底层：MapViewContainer
        MapViewContainer(
            modifier = Modifier.fillMaxSize(),
            locationViewModel = locationViewModel
        )

        // 上层：OutlinedTextField 放置在底部
        var text by remember { mutableStateOf("") }
        DestinationSearchCard(
            value = text,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            onSearchClick = onSearchClick,

        )
    }
}

    // 辅助 Composable：用于简洁显示当前定位信息文本
    @Composable
    fun CurrentLocationTextView(location: AMapLocation) {
        val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("当前位置信息:", style = MaterialTheme.typography.titleMedium)
            Text(
                "时间: ${sdf.format(Date(location.time))}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "来源: ${getLocationTypeString(location.locationType)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("经度: ${location.longitude}", style = MaterialTheme.typography.bodyMedium)
            Text("纬度: ${location.latitude}", style = MaterialTheme.typography.bodyMedium)
            Text("精度(米): ${location.accuracy}", style = MaterialTheme.typography.bodyMedium)
            if (location.address.isNotBlank()) {
                Text(
                    "地址: ${location.address}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    "地址信息获取中...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            if (location.errorCode != 0) {
                Text(
                    "定位错误码: ${location.errorCode}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Log.d(
                "MainScreen", "定位详情: \n" +
                        "纬度: ${location.latitude}\n" +
                        "经度: ${location.longitude}\n" +
                        "精度: ${location.accuracy}\n" +
                        "错误码: ${location.errorCode}\n" +
                        "地址: ${location.address}\n" +
                        "国家: ${location.country}\n" +
                        "省份: ${location.province}\n" +
                        "城市: ${location.city}\n" +
                        "区县: ${location.district}\n" +
                        "街道: ${location.street}\n" +
                        "街道号码: ${location.streetNum}\n" +
                        "POI名称: ${location.poiName}"
            )

        }
    }



@Composable
fun DestinationSearchCard(
    modifier: Modifier = Modifier,
    title: String = "搜索目的地",
    value: String,
    onSearchClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    placeholder = { Text("输入目的地") },
                    singleLine = true,
                    readOnly = true,
                    enabled = false,
                    shape = MaterialTheme.shapes.small.copy(
                        topStart = CornerSize(30.dp),
                        topEnd = CornerSize(30.dp),
                        bottomStart = CornerSize(30.dp),
                        bottomEnd = CornerSize(30.dp)
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable {
                            Log.d("MainScreen", "点击了搜索框")
                            onSearchClick()
                        },
                )
            }
        }
    }
}
@Composable
fun MapViewContainer(
    modifier: Modifier = Modifier,
    locationViewModel: LocationViewModel
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            map.uiSettings.isZoomControlsEnabled = true
        }
    }

    val locationResult by locationViewModel.locationResultState

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )


    // 设置地图初始位置
    LaunchedEffect(locationResult) {
        locationResult?.let { result ->
            val latLng = com.amap.api.maps.model.LatLng(
                result.latitude,
                result.longitude
            )
            mapView.map.moveCamera(
                com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
        }
    }

    // 生命周期绑定
    DisposableEffect(Unit) {
        mapView.onCreate(Bundle())
        onDispose {
            mapView.onDestroy()
        }
    }

    LaunchedEffect(Unit) {
        mapView.onResume()
    }
}


