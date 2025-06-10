package world.accera.dawn

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import world.accera.dawn.data.OriginDestination
import world.accera.dawn.data.RouteOptionSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanScreen(
    // *** 接收起终点经纬度和终点名称作为参数 (从导航参数传递而来) ***
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double,
    destName: String,

    // *** 获取 RoutePlanViewModel 实例 (通过 viewModel() 注入) ***
    routePlanViewModel: RoutePlanViewModel = viewModel(),

    // *** 返回按钮点击回调 ***
    onBackClick: () -> Unit,
    onNavigateStart: () -> Unit // 回调，传递方案ID
) {
    // 交通方式标签索引状态，初始选中步行 (索引 0)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val transportModes = listOf("步行", "公交", "驾车", "打车")

    // *** 观察 ViewModel 的状态 (真实数据、加载、错误) ***
    val allRouteData by routePlanViewModel.allRouteDataState // 观察规划结果数据
    val isLoading by routePlanViewModel.isLoadingState       // 观察加载状态
    val errorMessage by routePlanViewModel.errorMessageState   // 观察错误信息

    // *** 使用 LaunchedEffect 在屏幕组合时发起第一次算路 ***
    // Key on origin/destination/name parameters. 确保在这些参数变化时（通常只在首次进入时发生）触发算路
    LaunchedEffect(originLat, originLon, destLat, destLon, destName) {
        // 在 ViewModel 未加载且当前选中模式还没有数据时，发起算路
        // 避免重复算路或在 ViewModel 已有结果时再次算路
        // 这里的判断逻辑可以根据 ViewModel 的实际情况微调
        if (!isLoading && allRouteData[selectedTabIndex]?.isEmpty() != false) {
            Log.d("RoutePlanScreen", "LaunchedEffect: Triggering initial route planning for mode ${transportModes[selectedTabIndex]}")
            // 调用 ViewModel 的 planRoute 方法，传递起终点和当前选中模式
            routePlanViewModel.planRoute(originLat, originLon, destLat, destLon, destName, selectedTabIndex)
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("出行方案") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 应用 Scaffold 的 padding
                .padding(horizontal = 16.dp) // 页面内容左右内边距
        ) {
            // 起终点信息区域 (从传入参数构建 OriginDestination，用于UI展示)
            // TODO: 可以根据需要美化 OriginDestination 的文本显示，例如逆地理编码（可能在 ViewModel 中处理）
            val originDestination = OriginDestination(
                originText = "($originLat, $originLon)", // 暂时显示经纬度，后续可逆地理编码
                destinationText = destName
            )
            OriginDestinationInputSection(originDestination = originDestination)

            Spacer(modifier = Modifier.height(12.dp)) // 间距

            // 交通方式标签栏
            TransportModeTabs(
                transportModes = transportModes,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index ->
                    selectedTabIndex = index // 更新 UI 选中的标签状态
                    // *** 点击标签时，触发 ViewModel 规划新的交通方式 ***
                    // 调用 ViewModel 的 planRoute 方法，传递起终点和新的选中模式
                    Log.d("RoutePlanScreen", "Tab clicked: ${transportModes[index]}, Triggering planning")
                    routePlanViewModel.planRoute(originLat, originLon, destLat, destLon, destName, index)
                }
            )

            Spacer(modifier = Modifier.height(12.dp)) // 间距

            // 显示当前选中交通方式的路径方案列表 或 加载/错误提示
            Box(modifier = Modifier.fillMaxSize()) { // 使用 Box 来居中加载指示器和错误信息

                if (isLoading) {
                    // 显示加载指示器
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null) {
                    // 显示错误信息
                    Text(
                        text = "错误: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp)
                    )
                } else {
                    // 显示当前选中交通方式的路径方案列表
                    // 从 ViewModel 的 allRouteDataState 中获取当前选中模式的数据
                    val currentRoutes = allRouteData[selectedTabIndex] ?: emptyList()
                    if (currentRoutes.isEmpty()) {
                        // 如果当前交通方式没有方案，显示提示
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // 根据 selectedTabIndex 显示更具体的文本或通用文本
                            Text(
                                text = if (selectedTabIndex == 0) "未找到步行方案" else "未找到相关方案",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp) // 卡片间垂直间距
                        ) {
                            items(currentRoutes, key = { it.id }) { routeSummary -> // 使用方案ID作为key
                                // 传递从 ViewModel 获取的 RouteOptionSummary 数据
                                RouteOptionCard(
                                    routeSummary = routeSummary,
                                    onNavigateStart = onNavigateStart
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- OriginDestinationInputSection (保持不变) ---
@Composable
fun OriginDestinationInputSection(originDestination: OriginDestination) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = originDestination.originText,
            onValueChange = { /* 不处理输入 */ },
            readOnly = true, // 只读
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small.copy(
                topStart = CornerSize(16.dp),
                topEnd = CornerSize(16.dp),
                bottomStart = CornerSize(16.dp),
                bottomEnd = CornerSize(16.dp)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black
            )
        )
        //高度60dp
        OutlinedTextField(
            value = originDestination.destinationText,
            onValueChange = { /* 不处理输入 */ },
            readOnly = true, // 只读
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small.copy(
                topStart = CornerSize(16.dp),
                topEnd = CornerSize(16.dp),
                bottomStart = CornerSize(16.dp),
                bottomEnd = CornerSize(16.dp)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black
            )
        )

        Button(
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0000EB)),
            onClick = {
                // TODO: 实现调换起终点逻辑
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("调换起终点")
        }
    }
}


// --- TransportModeTabs (保持不变，onTabSelected 逻辑在 RoutePlanScreen 中实现) ---
@Composable
fun TransportModeTabs(
    transportModes: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit // 这个回调由 RoutePlanScreen 实现并传递进来
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth()
    ) {
        transportModes.forEachIndexed { index, mode ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) }, // 调用外部传入的回调
                text = { Text(mode) }
            )
        }
    }
}

// --- RouteOptionCard (保持不变) ---
@Composable
fun RouteOptionCard(
    routeSummary: RouteOptionSummary,
    // TODO: 接收点击按钮的回调
    onNavigateStart: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp) // 卡片内边距
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f) // 占据左侧大部分空间
                ) {
                    Text(
                        text = routeSummary.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = routeSummary.firstLineText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 辅助文字颜色
                    )
                }

                Button(
                    onClick = {
                        onNavigateStart()
                    },
                    modifier = Modifier.align(Alignment.CenterVertically) // 垂直居中对齐
                ) {
                    Text(routeSummary.buttonText) // 按钮文本来自数据
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // 分隔线

            Text(
                text = routeSummary.timeInfoText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}