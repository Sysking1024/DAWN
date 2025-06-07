package world.accera.dawn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.location.AMapLocation
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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 应用标题
        Text(
            text = "Demo",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp, top = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp)) // 增加标题下间距

        // 中间区域：展示当前定位信息文本
        // 使用 Column 嵌套，方便文本垂直排列
        Column(
            modifier = Modifier
                .weight(1f) // 占据中间剩余空间
                .fillMaxWidth()
                .padding(horizontal = 8.dp), // 左右内边距
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // 内容垂直居中
        ) {
            if (isLocating) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text("正在获取当前位置...")
            } else if (locationError != null) {
                Text(
                    text = "定位失败: ${locationError!!}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center // 居中显示
                )
                // 可以添加一个重试按钮
                // Button(onClick = { locationViewModel.startLocation(isOnce = true, needAddress = true) }) {
                //     Text("重试定位")
                // }
            } else if (locationResult != null) {
                CurrentLocationTextView(location = locationResult!!)
            } else {
                Text("点击下方搜索框开始定位并搜索")
            }
        }


        // 下方区域：搜索编辑框入口
        // 使用 OutlinedTextField 作为视觉表示，但使其可点击以触发导航
        OutlinedTextField(
            value = "", // 不显示实际输入值
            onValueChange = { /* 不处理输入 */ },
            label = { Text("搜索目的地") },
            readOnly = true, // 设置为只读，防止弹出键盘
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchClick() }, // 点击触发导航回调
            enabled = !isLocating // 定位进行中时禁用点击
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
        Text("时间: ${sdf.format(Date(location.time))}", style = MaterialTheme.typography.bodyMedium)
        Text("来源: ${getLocationTypeString(location.locationType)}", style = MaterialTheme.typography.bodyMedium)
        Text("经度: ${location.longitude}", style = MaterialTheme.typography.bodyMedium)
        Text("纬度: ${location.latitude}", style = MaterialTheme.typography.bodyMedium)
        Text("精度(米): ${location.accuracy}", style = MaterialTheme.typography.bodyMedium)
        if (location.address.isNotBlank()) {
            Text("地址: ${location.address}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        } else {
            Text("地址信息获取中...", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        if (location.errorCode != 0) {
            Text("定位错误码: ${location.errorCode}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
