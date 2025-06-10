package world.accera.dawn

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.services.core.PoiItem

@Composable
fun PoiSearchScreen(
    // 接收 PoiSearchViewModel
    poiViewModel: PoiSearchViewModel = viewModel(),
    // *** 接收 LocationViewModel，用于获取当前定位城市 ***
    locationViewModel: LocationViewModel = viewModel(),
    // TODO: 添加返回主界面的回调 onBackClick: () -> Unit = {}
    onPoiClick: (String) -> Unit
) {
    var keywordInput by remember { mutableStateOf("") } // 关键词输入，初始为空

    val isLoading by poiViewModel.isLoadingState
    val errorMessage by poiViewModel.errorMessageState
    val poiList = poiViewModel.poiListState
    val suggestionCities = poiViewModel.suggestionCitiesState // 仍然接收建议城市，但UI上不直接作为输入

    // *** 观察当前定位结果，用于获取城市信息 ***
    val locationResult by locationViewModel.locationResultState

    // *** 创建 FocusRequester ***
    val focusRequester = remember { FocusRequester() }
    // *** 获取 SoftwareKeyboardController ***
    val keyboardController = LocalSoftwareKeyboardController.current
    // *** 使用 LaunchedEffect 在屏幕进入组合时请求焦点并弹出键盘 ***
    LaunchedEffect(Unit) {
        Log.d("PoiSearchScreen", "屏幕组合完成，请求焦点并显示键盘")
        focusRequester.requestFocus() // 请求焦点
        keyboardController?.show() // 显示键盘
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEFF1F5))){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "搜索目的地", // 修改标题
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)
            )

            // 关键词输入框
            OutlinedTextField(
                value = keywordInput,
                onValueChange = { keywordInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .focusRequester(focusRequester),
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
                singleLine = true,
                enabled = !isLoading // 加载时禁用
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 搜索按钮
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0000EB)),
                onClick = {
                    // *** 获取当前定位城市代码，如果定位成功 ***
                    val currentCityCode = locationResult?.cityCode
                    if (currentCityCode.isNullOrBlank()) {
                        // 如果没有定位结果或城市信息，显示错误提示
                        // 可以在ViewModel中处理，也可以在这里设置UI状态
                        // 这里暂时直接给ViewModel一个空字符串，让ViewModel处理提示
                        Log.w("PoiSearchScreen", "未获取到当前定位城市，尝试不指定城市搜索...")
                        poiViewModel.searchPoi(keywordInput) // 不指定城市代码调用搜索
                    } else {
                        Log.d("PoiSearchScreen", "使用当前城市 ${locationResult?.city} (${currentCityCode}) 搜索...")
                        poiViewModel.searchPoi(keywordInput, currentCityCode) // 使用当前城市代码搜索
                    }

                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                // *** 当正在加载或当前定位城市信息不可用时，禁用搜索按钮 ***
                enabled = !isLoading // 如果没有城市信息，ViewModel内部会判断关键词，如果关键词非空，ViewModel可能依然尝试搜索
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("搜索") // 修改按钮文本
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 错误信息显示
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            // *** 移除城市建议的UI显示，但ViewModel仍可能返回关键词建议等，可以在错误信息中显示 ***
            /*if (suggestionCities.isNotEmpty()) {
                Text(
                    text = "或许你想在这些城市中搜索:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                suggestionCities.forEach { cityName ->
                    TextButton(
                        onClick = {
                            // cityInput = cityName // 城市输入框已移除，这个逻辑不再需要
                            // poiViewModel.searchPoi(keywordInput, cityName) // 直接用建议城市搜索
                        },
                        enabled = !isLoading
                    ) {
                        Text(cityName)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }*/

            // 搜索结果列表或加载/无结果提示
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading && poiList.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (poiList.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = poiList,
                            // 使用 id 作为 key 可以提高性能和稳定性
                            key = { poiItem -> poiItem.poiId }
                        ) { poiItem ->
                            // *** 调用 SimplePoiListItemView，并传递 onPoiClick 回调 ***
                            // *** 使用修改后的 PoiListItemView，或创建新的 Composable ***
                            SimplePoiListItemView(
                                poiItem = poiItem, // 调用新的或修改后的 Composable
                                onPoiClick = onPoiClick // 将从外部接收到的 onPoiClick 传递给列表项
                            )
                        }
                    }
                } else if (!isLoading && errorMessage == null) { // 仅在非加载、无错误且无结果时显示此文本
                    Text(
                        text = "请输入关键词并搜索", // 修改提示文本
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // 如果有错误信息，上面已经显示过了，这里不再重复
            }
        }
    }

}

// 1. 小票背景 Modifier
fun Modifier.ticketBackground(
    backgroundColor: Color = Color(0xFFFFFFFF),
    cornerRadius: Dp = 12.dp,
    notchRadius: Dp = 10.dp
): Modifier = this.then(
    Modifier
        .clip(TicketShape(cornerRadius, notchRadius))
        .background(backgroundColor)
)

class TicketShape(
    private val cornerRadius: Dp,
    private val notchRadius: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val corner = with(density) { cornerRadius.toPx() }
        val notch = with(density) { notchRadius.toPx() }
        val path = Path().apply {
            // Top left corner
            arcTo(
                rect = Rect(0f, 0f, corner * 2, corner * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Top edge
            lineTo(size.width - corner, 0f)
            // Top right corner
            arcTo(
                rect = Rect(size.width - corner * 2, 0f, size.width, corner * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Right notch
            arcTo(
                rect = Rect(size.width - notch, size.height / 2 - notch, size.width + notch, size.height / 2 + notch),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Bottom right corner
            lineTo(size.width, size.height - corner)
            arcTo(
                rect = Rect(size.width - corner * 2, size.height - corner * 2, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Bottom edge
            lineTo(corner, size.height)
            // Bottom left corner
            arcTo(
                rect = Rect(0f, size.height - corner * 2, corner * 2, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Left notch
            arcTo(
                rect = Rect(-notch, size.height / 2 - notch, notch, size.height / 2 + notch),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

// 3. 虚线分割线
@Composable
fun DashedDivider(
    color: Color = Color.LightGray,
    thickness: Dp = 1.dp,
    dashWidth: Dp = 6.dp,
    gapWidth: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
    ) {
        val totalWidth = size.width
        val dash = dashWidth.toPx()
        val gap = gapWidth.toPx()
        var startX = 0f
        while (startX < totalWidth) {
            drawLine(
                color = color,
                start = Offset(startX, 0f),
                end = Offset((startX + dash).coerceAtMost(totalWidth), 0f),
                strokeWidth = thickness.toPx()
            )
            startX += dash + gap
        }
    }
}

// 4. 小票风格的 POI 列表项
@Composable
fun SimplePoiListItemView(
    poiItem: PoiItem,
    onPoiClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .ticketBackground()
            .clickable { onPoiClick(poiItem.poiId) }
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Text(
            text = poiItem.title ?: "无标题信息",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        DashedDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "地址：${poiItem.snippet ?: "无地址信息"}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 原始的 PoiListItemView 可以保留或移除，取决于后续是否在POI详情页复用
// 如果在详情页复用，可以考虑重命名或调整其参数以适应详情页的展示需求
/*
@Composable
fun PoiListItemView(poiItem: PoiItem, modifier: Modifier = Modifier) {
    Log.d("MainActivity", "地址信息：${poiItem.title}")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = poiItem.title ?: "无标题信息",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "地址：${poiItem.snippet ?: "无地址信息"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "类型: ${poiItem.typeDes ?: "未知类型"}",
            style = MaterialTheme.typography.bodySmall
        )
        if (!poiItem.tel.isNullOrBlank()) {
            Text(
                text = "电话: ${poiItem.tel}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
*/
