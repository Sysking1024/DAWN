package world.accera.dawn

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import world.accera.dawn.data.OriginDestination
import world.accera.dawn.data.RouteOptionSummary
import world.accera.dawn.ui.components.TicketContainer

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

    // 添加内部状态管理
    var internalOriginLat by remember { mutableStateOf(originLat) }
    var internalOriginLon by remember { mutableStateOf(originLon) }
    var internalDestLat by remember { mutableStateOf(destLat) }
    var internalDestLon by remember { mutableStateOf(destLon) }
    var internalDestName by remember { mutableStateOf(destName) }

    // 添加起点名称状态（默认为经纬度）
    var originName by remember { mutableStateOf("($originLat, $originLon)") }

    // 交换起终点的逻辑
    val swapOriginDestination = {
        // 交换经纬度
        val tempLat = internalOriginLat
        val tempLon = internalOriginLon
        internalOriginLat = internalDestLat
        internalOriginLon = internalDestLon
        internalDestLat = tempLat
        internalDestLon = tempLon

        // 交换名称
        val tempName = originName
        originName = internalDestName
        internalDestName = tempName

        // 触发重新规划路径
        routePlanViewModel.planRoute(
            internalOriginLat,
            internalOriginLon,
            internalDestLat,
            internalDestLon,
            internalDestName,
            selectedTabIndex
        )
    }


    // 初始化规划
    LaunchedEffect(Unit) {
        if (!isLoading && allRouteData[selectedTabIndex]?.isEmpty() != false) {
            routePlanViewModel.planRoute(
                internalOriginLat,
                internalOriginLon,
                internalDestLat,
                internalDestLon,
                internalDestName,
                selectedTabIndex
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF1F5))
    ) {
        // 顶部栏
        CenterAlignedTopAppBar(
            title = { Text("出行方案") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFFEFF1F5) // 这里设置为你想要的背景色
            )
        )

        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp) // 你可以根据需要调整顶部 padding
        ) {
            // 起终点信息区域
            val originDestination = OriginDestination(
                originText = "($originLat, $originLon)",
                destinationText = destName
            )
            OriginDestinationInputSection(
                originName = originName,
                destinationName = internalDestName,
                onSwapClick = swapOriginDestination
            )

            Spacer(modifier = Modifier.height(12.dp))

            val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(8.dp, shape, clip = false)
                    .clip(shape)
                    .background(Color(0xFFEEF0FB))
                    .padding(20.dp)
            ) {
                // 交通方式标签栏
                TransportModeTabs(
                    transportModes = transportModes,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        selectedTabIndex = index
                        Log.d(
                            "RoutePlanScreen",
                            "Tab clicked: ${transportModes[index]}, Triggering planning"
                        )
                        routePlanViewModel.planRoute(
                            originLat,
                            originLon,
                            destLat,
                            destLon,
                            destName,
                            index
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (errorMessage != null) {
                        Text(
                            text = "错误: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp)
                        )
                    } else {
                        val currentRoutes = allRouteData[selectedTabIndex] ?: emptyList()
                        if (currentRoutes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedTabIndex == 0) "未找到步行方案" else "未找到相关方案",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    currentRoutes,
                                    key = { it.id }
                                ) { routeSummary ->
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
}

// --- OriginDestinationInputSection (保持不变) ---
@Composable
fun OriginDestinationInputSection(
    originName: String,
    destinationName: String,
    onSwapClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = originName, // 使用起点名称
            onValueChange = { /* 不处理输入 */ },
            readOnly = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = MaterialTheme.shapes.small.copy(
                topStart = CornerSize(30.dp),
                topEnd = CornerSize(30.dp),
                bottomStart = CornerSize(30.dp),
                bottomEnd = CornerSize(30.dp)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black
            )
        )


        OutlinedTextField(
            value = destinationName, // 使用目的地名称
            onValueChange = { /* 不处理输入 */ },
            readOnly = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = MaterialTheme.shapes.small.copy(
                topStart = CornerSize(30.dp),
                topEnd = CornerSize(30.dp),
                bottomStart = CornerSize(30.dp),
                bottomEnd = CornerSize(30.dp)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black
            )
        )

        Button(
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0000EB)),
            onClick = onSwapClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
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
    onTabSelected: (Int) -> Unit
) {
    // 圆角半径，pill形状
    val cornerRadius = 24.dp

    // 外层包裹，设置黑色边框和圆角
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White) // 背景色可根据需要调整
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            transportModes.forEachIndexed { index, mode ->
                val isSelected = selectedTabIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(if (isSelected) Color(0xFFFFB1EE) else Color.Transparent)
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
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
    TicketContainer(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    modifier = Modifier.align(Alignment.CenterVertically),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0000EB),
                        contentColor = Color.White
                    )
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