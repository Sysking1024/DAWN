package world.accera.dawn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amap.api.services.core.PoiItem

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
    // 使用 Box 布局，将详情卡片固定在底部
    Box(modifier = Modifier.fillMaxSize()) {
        // 上部区域留空给地图图层 (目前无需实现地图)
        // 可以使用 Spacer 或 Modifier.weight(1f) 在 Column 中实现，
        // 但 Box 更适合将卡片精确对齐到底部
        Spacer(modifier = Modifier
            .fillMaxWidth()
        )

        // POI 详情卡片 (固定在底部)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                // .height(IntrinsicSize.Max) // 如果内容高度不固定，可以尝试
                .align(Alignment.BottomCenter) // 对齐到底部中心
                .padding(16.dp) // 卡片外边距
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp) // 卡片内边距
            ) {
                // 卡片顶部标题区域 (返回按钮 + POI名称)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // 子元素之间有间距
                ) {
                    // 返回按钮
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }

                    // POI 名称
                    Text(
                        text = poiItem.title ?: "未知地点",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f) // 占据剩余空间，允许标题换行
                            .padding(horizontal = 8.dp) // 左右内边距
                    )

                    // “到这去”按钮
                    Button(
                        onClick = {
                            // *** 调用 onNavigateToRoutePlan 回调，传递当前定位和目标 POI ***
                            onNavigateToRoutePlan(originLatLon, poiItem)
                        },
                        // 可以根据需要设置按钮的 enabled 状态
                        // enabled = !navigateViewModel.isLoadingState.value
                    ) {
                        Text("到这去")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // 分隔线

                // 卡片主要展示区域 (所有 POI 详细信息)
                PoiDetailContentView(poiItem = poiItem)
            }
        }
    }
}

// *** 辅助 Composable：用于显示 POI 的完整详细信息 ***
// 这与原始的 PoiListItemView 相似，但展示更多信息
@Composable
fun PoiDetailContentView(poiItem: PoiItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp) // 元素间垂直间距
    ) {
        InfoRow("标题:", poiItem.title)
        InfoRow("地址:", poiItem.snippet)
        InfoRow("类型:", poiItem.typeDes)
        if (!poiItem.tel.isNullOrBlank()) {
            InfoRow("电话:", poiItem.tel)
        }
        // 经纬度信息也可以展示，如果需要的话
        // InfoRow("经度:", poiItem.latLonPoint.longitude.toString())
        // InfoRow("纬度:", poiItem.latLonPoint.latitude.toString())
        InfoRow("POI ID:", poiItem.poiId) // 可能用于调试或进一步操作
        // ... 其他您认为需要展示的 PoiItem 属性
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
//        PoiDetailScreen(poiItem = dummyPoiItem, onBackClick = {})
//    }
//}