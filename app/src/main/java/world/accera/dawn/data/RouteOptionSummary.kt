package world.accera.dawn.data

import com.amap.api.navi.model.AMapNaviPath

data class RouteOptionSummary(
    val id: String, // 方案唯一标识 (可以使用路径的hashCode或生成的UUID)
    val title: String, // 卡片标题，如“方案1”
    val firstLineText: String, // 标题下方第一行文本，根据交通方式变化
    val timeInfoText: String, // 包含距离、时间、到达时间的文本组合
    val buttonText: String, // 右侧按钮文本 ("出发" 或 "轿车")
    val path: AMapNaviPath? = null // 存储原始路径对象，方便后续使用 (如开始导航)
    // val routeData: Any? = null // 可以存储原始的SDK返回的路线对象，更通用
)