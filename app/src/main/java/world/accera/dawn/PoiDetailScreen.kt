package world.accera.dawn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.PoiItem
import com.amap.api.maps.MapView
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.CameraUpdateFactory
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color

@Composable
fun PoiDetailScreen(
    // *** 接收要显示的 PoiItem 数据 ***
    // 在集成导航时，这里通常会接收 POI ID，然后在 ViewModel 中查找对应的 PoiItem
    // 但为了UI开发和演示方便，这里先假设直接接收 PoiItem 对象
    poiItem: PoiItem,
    // *** 添加接收当前定位经纬度的参数 ***
    originLatLon: Pair<Double, Double>, // 使用 Pair<Double, Double> 传递经纬度
    // *** 返回按钮点击时的回调 ***
    onBackClick: () -> Unit,
    // *** 添加点击“到这去”按钮时的回调，传递起终点信息 ***
    // 起点使用经纬度 Pair，终点传递完整的 PoiItem
    onNavigateToRoutePlan: (originLatLon: Pair<Double, Double>, destinationPoi: PoiItem) -> Unit
) {
    val context = LocalContext.current
    val cameraLatLng = remember(poiItem) {
        LatLng(poiItem.latLonPoint.latitude, poiItem.latLonPoint.longitude)
    }
    val mapView = remember {
        MapView(context).apply {
            // It's crucial to call onCreate here or in the factory of AndroidView
            onCreate(null) // Call onCreate
        }
    }

    // 使用 Box 布局，将详情卡片固定在底部
    Box(modifier = Modifier.fillMaxSize()) {
        // 高德地图作为背景
        AndroidView(
            factory = { mapView }, // Use the remembered MapView instance
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(cameraLatLng, 15f))
                view.getMap().clear() // 清除旧标记（如果之前有的话）
                view.getMap().addMarker(
                    com.amap.api.maps.model.MarkerOptions()
                        .position(cameraLatLng)
                        .title(poiItem.title)
                        .snippet(poiItem.snippet)
                )
            }
        )
        // Lifecycle management for MapView
        DisposableEffect(mapView) {
            onDispose {
                mapView.onPause() // Call onPause before onDestroy
                mapView.onDestroy()
            }
        }

        // Call onResume when the composable is first launched and when it resumes
        // This is a simplified way; for more complex scenarios, consider LocalLifecycleOwner
        LaunchedEffect(mapView) {
            mapView.onResume()
        }

        // POI 详情卡片 (固定在底部)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // 对齐到底部中心
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp) // 卡片内边距
            ) {
                // 卡片主要展示区域 (所有 POI 详细信息)
                PoiDetailContentView(
                    poiItem = poiItem,
                    onBackClick = onBackClick, // Pass the onBackClick lambda
                    onNavigateClick = {
                        onNavigateToRoutePlan(
                            originLatLon,
                            poiItem
                        )
                    } // Pass the navigation lambda
                )
            }
        }
    }
}

// *** 辅助 Composable：用于显示 POI 的完整详细信息 ***
@Composable
fun PoiDetailContentView(
    poiItem: PoiItem,
    onBackClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally // Center content like title
    ) {
        // POI 主标题
        Text(
            text = poiItem.title ?: "未知地点",
            style = MaterialTheme.typography.headlineSmall, // Larger title
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 详细信息行
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp) // 元素间垂直间距
        ) {
            InfoRow("地址:", poiItem.snippet)
            InfoRow("类型:", poiItem.typeDes)
            if (!poiItem.tel.isNullOrBlank()) {
                InfoRow("电话:", poiItem.tel)
            }
            InfoRow("POI ID:", poiItem.poiId)
        }

        Spacer(modifier = Modifier.height(16.dp)) // Spacer before buttons

        // 底部操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly // Distribute buttons evenly
        ) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                border = BorderStroke(1.dp, Color(0xFF0000EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0000EB),
                    containerColor = Color.White
                )
            ) {
                Text("返回")
            }
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0000EB)),
                onClick = onNavigateClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                // ButtonDefaults.buttonColors(containerColor = Color.Blue) // Example for specific blue
            ) {
                Text("到这去")
            }
        }
    }
}

// InfoRow 辅助 Composable（如果还没有的话，从 LocationScreen.kt 或 LocationUtils.kt 复制过来）
// 如果已经放在 LocationUtils.kt 中并通过 import 引入，则无需重复定义
/*
@Composable
fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(100.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
*/

// Preview (可选)
//@Preview(showBackground = true)
//@Composable
//fun PreviewPoiDetailScreen() {
//    // 创建一个假的 PoiItem 数据用于预览
//    val dummyPoiItem = PoiItem("123", LatLonPoint(39.908823, 116.397405), "测试POI名称", "测试地址片段")
//    dummyPoiItem.tel = "1234567890"
//    dummyPoiItem.typeDes = "测试类型"
//
//    DAWNTheme { // 假设你的主题可用
//        PoiDetailScreen(poiItem = dummyPoiItem, onBackClick = {}, originLatLon = Pair(0.0,0.0), onNavigateToRoutePlan = {_,_ ->})
//    }
//}