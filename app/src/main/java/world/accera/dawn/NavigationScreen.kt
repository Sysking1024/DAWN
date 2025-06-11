package world.accera.dawn

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.navi.AMapNaviView

@Composable
fun NavigationScreen(
    // 获取 RoutePlanViewModel 实例，用于访问 AMapNavi 和启动导航
    routePlanViewModel: RoutePlanViewModel = viewModel(),
    // TODO: 如果需要返回按钮，添加 onBackClick 回调
    // onBackClick: () -> Unit = {}
    onEndNaviTask: () -> Unit = {}
) {
    val context = LocalContext.current
    //val lifecycle = rememberLifecycleEvent() // 监听 Compose 生命周期事件

    // 使用 remember 存储 AMapNaviView 实例
    var aMapNaviView by remember { mutableStateOf<AMapNaviView?>(null) }

    // 导航结束对话框显示状态
    val showDialog by routePlanViewModel.showCameraDialog.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ){
        // 使用 AndroidView 将 AMapNaviView 嵌入到 Compose 中
        AndroidView(
            factory = { ctx ->
                // *** 创建 AMapNaviView 实例 ***
                // 这里的 FrameLayout 只是为了提供一个容器，你也可以直接创建 AMapNaviView
                // 更标准的做法可能是通过 LayoutInflater 膨胀一个包含 AMapNaviView 的 XML 布局
                // 例如：LayoutInflater.from(ctx).inflate(R.layout.activity_basic_navi, null) as FrameLayout
                // 然后找到其中的 AMapNaviView
                // 为了简化，这里直接创建 AMapNaviView
                AMapNaviView(ctx).apply {
                    // AMapNaviView 的 onCreate, onResume, onPause, onDestroy
                    // 需要在 Compose 的生命周期中管理，使用 DisposableEffect
                    // onCreate(null) // 在 DisposableEffect 中调用
                    // onResume() // 在 DisposableEffect 中调用
                    setAMapNaviViewListener(routePlanViewModel) // *** 设置 ViewModel 为 AMapNaviViewListener ***
                    naviMode = AMapNaviView.NORTH_UP_MODE

                    // 存储 AMapNaviView 引用，方便在 DisposableEffect 中访问
                    aMapNaviView = this
                }
            },
            modifier = Modifier.fillMaxSize(), // 让导航视图填充整个屏幕
            update = { view ->
                // AndroidView 的 update 块，当状态变化导致重组时调用
                // 可以在这里更新 View 的属性，但 AMapNaviView 的大多数配置在创建时完成
            }
        )

        val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape, clip = false)
                .clip(shape)
                .background(Color(0xFFEEF0FB))
                .padding(20.dp)
                .align(Alignment.BottomCenter)
        ) {
            val remainingTime by routePlanViewModel.remainingTime.collectAsState()
            val remainingDistance by routePlanViewModel.remainingDistance.collectAsState()
            val formattedArrivalTime by routePlanViewModel.arrivalTime.collectAsState()

            Text(
                text =  "$remainingDistance，$remainingTime，$formattedArrivalTime",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 12.dp, top = 12.dp).align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )

            CenteredButtonRow(
                onExitClick = { routePlanViewModel.exitNavigation() },
                onSettingsClick = { routePlanViewModel.openSettings() },
                onOverviewClick = { routePlanViewModel.requestOverview() }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // 使用 DisposableEffect 管理 AMapNaviView 的生命周期
    // Keyed on aMapNaviView instance
    DisposableEffect(aMapNaviView) {
        val view = aMapNaviView

        // 在 Composable 进入组合时调用 onCreate 和 onResume
        view?.onCreate(null)
        view?.onResume()
        Log.d("NavigationScreen", "AMapNaviView onCreate and onResume called")

        // *** 通知 ViewModel AMapNaviView 已就绪，并传递路线 ID ***
        // ViewModel 接收到这个信号后，如果 AMapNavi 实例也准备好了，就发起 startNavi
        routePlanViewModel.onRouteOptionSelectedForNavigation()


        onDispose {
            // 在 Composable 退出组合时调用 onPause 和 onDestroy
            Log.d("NavigationScreen", "AMapNaviView onPause and onDestroy called")
            view?.onPause()
            view?.onDestroy()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { routePlanViewModel.onCameraDialogDismissed() },
            title = { Text("提示") },
            text = { Text("导航已结束，是否需要启动相机识别周围环境？") },
            confirmButton = {
                Button(
                    onClick = onEndNaviTask
                ) {
                    Text("是")
                }
            },
            dismissButton = {
                Button(onClick = { routePlanViewModel.onCameraDialogDismissed() }) {
                    Text("否")
                }
            }
        )
    }

    // TODO: 添加其他 UI 元素，例如返回按钮 (如果需要的话)
    // 可以覆盖 AMapNaviView 的一部分或添加到布局中

    // Optional: Add a simple back button overlay
    /*
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回"
            )
        }
    }
    */
}


@Composable
fun CenteredButtonRow(
    onExitClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOverviewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(43.dp)) // 左右半圆
            .background(Color(0xFF0000EB))
            .padding(10.dp), // 可自定义背景色
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTextButton(
            painter = painterResource(id = R.mipmap.nav_exit),
            text = "退出",
            onClick = onExitClick
        )
        Spacer(modifier = Modifier.width(32.dp))
        IconTextButton(
            painter = painterResource(id = R.mipmap.nav_setting),
            text = "设置",
            onClick = onSettingsClick
        )
        Spacer(modifier = Modifier.width(32.dp))
        IconTextButton(
            painter = painterResource(id = R.mipmap.open_in_full),
            text = "全览",
            onClick = onOverviewClick
        )
    }
}


@Composable
fun IconTextButton(
    painter: Painter,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = painter,
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}


// --- 辅助 Composable：监听 Compose 生命周期事件 (如果需要精确控制生命周期方法) ---
// 这个辅助函数可以帮助你更精确地在 onResume/onPause 时调用 View 方法
// 但对于 AMapNaviView，通常直接在 DisposableEffect 的创建和 onDispose 中处理就够了
// @Composable
// fun rememberLifecycleEvent(): Lifecycle.Event {
//     val lifecycleOwner = LocalLifecycleOwner.current
//     val state = remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
//     DisposableEffect(lifecycleOwner) {
//         val observer = LifecycleEventObserver { _, event ->
//             state.value = event
//         }
//         lifecycleOwner.lifecycle.addObserver(observer)
//         onDispose {
//             lifecycleOwner.lifecycle.removeObserver(observer)
//         }
//     }
//     return state.value
// }