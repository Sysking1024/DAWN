package world.accera.dawn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import world.accera.dawn.ui.theme.DAWNTheme

// *** 定义导航路由常量，调整顺序和语法 ***

// 基础路由
const val MainRoute = "main_screen"
const val PoiSearchRoute = "poi_search_screen"
const val PoiDetailRoute = "poi_detail_screen" // 详情页的基础路由
const val RoutePlanRoute = "route_plan_screen" // 出行方案页基础路由
const val NavigationRoute = "navigation_screen"
const val CameraRecognitionRoute = "camera_recognition_screen"
const val ChatScreenRoute = "chat_screen"

// 参数名常量 - 先定义所有参数名常量
const val PoiIdNavArg = "poiId" // 用于传递 POI ID 的导航参数名称
const val OriginLatNavArg = "originLat" // Origin 经度参数名
const val OriginLonNavArg = "originLon" // Origin 纬度参数名

// 出行方案参数名常量
const val RoutePlanOriginLatArg = "rpOriginLat" // 出行方案起点经度参数名
const val RoutePlanOriginLonArg = "rpOriginLon" // 出行方案起点纬度参数名
const val RoutePlanDestLatArg = "rpDestLat" // 出行方案终点经度参数名
const val RoutePlanDestLonArg = "rpDestLon" // 出行方案终点纬度参数名
const val RoutePlanDestNameArg = "rpDestName" // 出行方案终点名称参数名

// 带参数的路由 - 后定义，使用上面定义的参数名常量
// POI 详情页带参数路由
const val PoiDetailRouteWithArgs =
    "$PoiDetailRoute/{$PoiIdNavArg}?$OriginLatNavArg={$OriginLatNavArg}&$OriginLonNavArg={$OriginLonNavArg}"

// 出行方案页带参数路由，使用明确的字符串模板语法
const val RoutePlanRouteWithArgs =
    "$RoutePlanRoute?${RoutePlanOriginLatArg}={${RoutePlanOriginLatArg}}&${RoutePlanOriginLonArg}={${RoutePlanOriginLonArg}}&${RoutePlanDestLatArg}={${RoutePlanDestLatArg}}&${RoutePlanDestLonArg}={${RoutePlanDestLonArg}}&${RoutePlanDestNameArg}={${RoutePlanDestNameArg}}"

class MainActivity : ComponentActivity() {

    // 1. 前台权限（不含后台定位）
    private val foregroundPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        Manifest.permission.FOREGROUND_SERVICE
    )

    // 2. 后台定位权限
    private val backgroundPermissions = arrayOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    // 记录是否已经请求过后台定位，避免重复弹窗
    private var hasRequestedBackgroundLocation = false

    private val requestForegroundPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            Log.d("PERMISSION", "前台权限回调, allGranted=$allGranted, permissions=$permissions")
            if (allGranted) {
                // 前台权限全部授予，主动触发定位
                locationViewModel.startLocation(isOnce = true, needAddress = true)
                Log.d("PERMISSION", "前台权限全部授予，调用 startLocation")
                // 检查是否需要请求后台定位
                requestBackgroundLocationIfNeeded()
            } else {
                // 权限被拒绝
                locationViewModel.locationErrorState.value = "请授予所有权限以正常使用定位功能"
                Log.d("PERMISSION", "前台权限未全部授予，不调用 startLocation")
            }
        }

    // 后台定位权限请求
    private val requestBackgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            Log.d(
                "PERMISSION",
                "后台定位权限回调, allGranted=$allGranted, permissions=$permissions"
            )
            if (allGranted) {
                // 后台定位权限授予后，主动触发定位
                locationViewModel.startLocation(isOnce = true, needAddress = true)
                Log.d("PERMISSION", "后台定位权限全部授予，调用 startLocation")
            } else {
                locationViewModel.locationErrorState.value = "请授予后台定位权限以支持后台定位"
                Log.d("PERMISSION", "后台定位权限未全部授予，不调用 startLocation")
            }
        }


    private val locationViewModel: LocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(
            "PERMISSION",
            "onCreate, hasAllForegroundPermissions=${hasAllForegroundPermissions()}, hasBackgroundLocationPermission=${hasBackgroundLocationPermission()}"
        )

        // 启动时自动检测并请求权限
        if (!hasAllForegroundPermissions()) {
            Log.d("PERMISSION", "请求前台权限")
            requestForegroundPermissionsLauncher.launch(foregroundPermissions)
        } else {
            // 已有前台权限，直接开始定位
            locationViewModel.startLocation(isOnce = true, needAddress = true)
            // 检查是否需要请求后台定位
            requestBackgroundLocationIfNeeded()
        }

        setContent {
            DAWNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // *** 创建 NavController ***
                    val navController = rememberNavController()

                    // 获取 ViewModel 实例
                    // ViewModel 会在 NavHost 的生命周期内被正确管理
//                    val locationViewModel: LocationViewModel = viewModel()
                    val poiSearchViewModel: PoiSearchViewModel = viewModel()
                    val routePlanViewModel: RoutePlanViewModel = viewModel()
                    val chatViewModel: ChatViewModel = viewModel()


                    // *** 设置 NavHost，定义导航图 ***
                    NavHost(navController = navController, startDestination = MainRoute) {
                        /**                        composable(ChatScreenRoute) {
                        //                            ChatScreen(viewModel = chatViewModel)
                        //                        }**/

                        // 主界面
                        composable(MainRoute) {
                            MainScreen(
                                locationViewModel = locationViewModel, // 传递 LocationViewModel
                                onSearchClick = {
                                    // *** 在点击搜索框时触发导航到 POI 搜索界面 ***
                                    navController.navigate(PoiSearchRoute)
                                    Log.d("MainActivity", "导航到 POI 搜索界面")
                                }
                            )
                        }

                        // POI 搜索界面
                        composable(PoiSearchRoute) { backStackEntry ->
                            PoiSearchScreen(
                                poiViewModel = poiSearchViewModel, // 传递 PoiSearchViewModel
                                locationViewModel = locationViewModel, // 传递 LocationViewModel 获取当前城市
                                // TODO: 添加返回主界面的回调 onBackClick 参数给 PoiSearchScreen
                                // onBackClick = { navController.popBackStack() } // 返回上一级
                                // *** 传递 onPoiClick 回调给 PoiSearchScreen ***
                                onPoiClick = { clickedPoiId ->
                                    Log.d(
                                        "MainActivity",
                                        "在搜索结果页点击了 POI ID: $clickedPoiId"
                                    )

                                    // *** 在导航到详情页时，获取当前定位并作为参数传递 ***
                                    val currentLocation =
                                        locationViewModel.locationResultState.value
                                    val originLat = currentLocation?.latitude?.toFloat() ?: 0.0f
                                    val originLon = currentLocation?.longitude?.toFloat() ?: 0.0f
                                    // *** 构建带参数的路由并导航到 POI 详情页 ***
                                    navController.navigate("$PoiDetailRoute/$clickedPoiId?$OriginLatNavArg=$originLat&$OriginLonNavArg=$originLon")
                                }
                            )
                        }

                        // *** POI 详情界面 Composable，定义接收 POI ID 和 Origin 经纬度参数 ***
                        composable(
                            route = PoiDetailRouteWithArgs, // 使用包含参数占位符的路由
                            arguments = listOf(
                                // 定义 poiId 参数，类型为 String
                                navArgument(PoiIdNavArg) { type = NavType.StringType },
                                // 定义 Origin 经纬度参数为 FloatType，并提供默认值
                                navArgument(OriginLatNavArg) {
                                    type = NavType.FloatType
                                    defaultValue = 0.0f // 提供默认值，尽管定位通常不会是 0,0
                                },
                                navArgument(OriginLonNavArg) {
                                    type = NavType.FloatType
                                    defaultValue = 0.0f
                                }
                            )
                        ) { backStackEntry ->
                            // *** 从导航参数中获取 POI ID 和 Origin 经纬度 ***
                            val poiId = backStackEntry.arguments?.getString(PoiIdNavArg)
                            val originLat =
                                backStackEntry.arguments?.getFloat(OriginLatNavArg) ?: 0.0f
                            val originLon =
                                backStackEntry.arguments?.getFloat(OriginLonNavArg) ?: 0.0f

                            Log.d(
                                "MainActivity",
                                "进入 POI 详情页，获取到 POI ID: $poiId, Origin LatLon(Float): ($originLat, $originLon)"
                            )

                            // *** 根据 POI ID 从 PoiSearchViewModel 中查找对应的 PoiItem ***
                            // 注意：这里假设 PoiSearchViewModel 的生命周期覆盖了 PoiDetailScreen
                            val poiItem = poiId?.let {
                                // 使用之前在 PoiSearchViewModel 中添加的函数查找
                                poiSearchViewModel.getPoiItemById(it)
                            }

                            // *** 显示 PoiDetailScreen，如果找到了 PoiItem ***
                            if (poiItem != null) {
                                PoiDetailScreen(
                                    poiItem = poiItem, // 将找到的 PoiItem 数据传递给详情页 UI
                                    originLatLon = Pair(
                                        originLat.toDouble(),
                                        originLon.toDouble()
                                    ), // 将 Float 转换为 Double 传递给屏幕 Composable
                                    onBackClick = {
                                        // *** 点击详情页的返回按钮时，返回到上一级 (搜索结果页) ***
                                        navController.popBackStack()
                                        Log.d("MainActivity", "从 POI 详情页返回搜索结果页")
                                    },
                                    // *** 实现 onNavigateToRoutePlan 回调，用于导航到出行方案页 ***
                                    onNavigateToRoutePlan = { originPoint, destinationPoi ->
                                        Log.d(
                                            "MainActivity",
                                            "从详情页点击到这去，准备导航到出行方案页"
                                        )

                                        // 导航到出行方案页，传递起终点信息作为参数
                                        // 终点使用 POI 的经纬度
                                        val destLat =
                                            destinationPoi.latLonPoint.latitude.toFloat() // 同样使用 Float
                                        val destLon =
                                            destinationPoi.latLonPoint.longitude.toFloat() // 同样使用 Float
                                        val destName =
                                            destinationPoi.title ?: "目的地" // 使用 POI 名称作为终点名称

                                        // 构建带参数的出行方案路由并导航
                                        navController.navigate(
                                            "$RoutePlanRoute?$RoutePlanOriginLatArg=${originPoint.first.toFloat()}&$RoutePlanOriginLonArg=${originPoint.second.toFloat()}&$RoutePlanDestLatArg=$destLat&$RoutePlanDestLonArg=$destLon&$RoutePlanDestNameArg=$destName"
                                        ) {
                                            // 可选：配置导航行为，例如，不将详情页添加到返回栈
                                            // popUpTo(PoiDetailRoute) { inclusive = true }
                                        }
                                    }
                                )
                            } else {
                                // *** 处理找不到对应 PoiItem 的情况 ***
                                // 例如，显示一个错误消息，或者延迟后自动返回
                                Log.e("MainActivity", "未在 ViewModel 中找到对应 ID 的 POI: $poiId")
                                // 可以在这里显示一个错误提示
                                //Text("未找到该POI详情或数据已失效", color = MaterialTheme.colorScheme.error)
                                // 也可以考虑在短时间后自动返回到搜索结果页
                                // LaunchedEffect(Unit) { delay(1500); navController.popBackStack() }
                            }
                        }

                        // *** 出行方案界面 Composable，定义接收起终点参数 ***
                        composable(
                            route = RoutePlanRouteWithArgs, // 使用包含参数占位符的路由
                            arguments = listOf(
                                navArgument(RoutePlanOriginLatArg) {
                                    type = NavType.FloatType; defaultValue = 0.0f
                                },
                                navArgument(RoutePlanOriginLonArg) {
                                    type = NavType.FloatType; defaultValue = 0.0f
                                },
                                navArgument(RoutePlanDestLatArg) {
                                    type = NavType.FloatType; defaultValue = 0.0f
                                },
                                navArgument(RoutePlanDestLonArg) {
                                    type = NavType.FloatType; defaultValue = 0.0f
                                },
                                navArgument(RoutePlanDestNameArg) {
                                    type = NavType.StringType; defaultValue = "目的地"
                                } // 终点名称参数
                            )
                        ) { backStackEntry ->
// *** 从导航参数中获取起终点信息 ***
                            val originLat =
                                backStackEntry.arguments?.getFloat(RoutePlanOriginLatArg) ?: 0.0f
                            val originLon =
                                backStackEntry.arguments?.getFloat(RoutePlanOriginLonArg) ?: 0.0f
                            val destLat =
                                backStackEntry.arguments?.getFloat(RoutePlanDestLatArg) ?: 0.0f
                            val destLon =
                                backStackEntry.arguments?.getFloat(RoutePlanDestLonArg) ?: 0.0f
                            val destName = backStackEntry.arguments?.getString(RoutePlanDestNameArg)
                                ?: "目的地"

                            Log.d(
                                "MainActivity",
                                "进入出行方案页，获取到 Origin LatLon(Float): ($originLat, $originLon), Dest LatLon(Float): ($destLat, $destLon), Dest Name: $destName"
                            )

                            RoutePlanScreen(
                                originLat = originLat.toDouble(), // Float 转 Double 给 ViewModel
                                originLon = originLon.toDouble(), // Float 转 Double 给 ViewModel
                                destLat = destLat.toDouble(),     // Float 转 Double 给 ViewModel
                                destLon = destLon.toDouble(),     // Float 转 Double 给 ViewModel
                                destName = destName,              // 终点名称
                                routePlanViewModel = routePlanViewModel, // *** 传递 ViewModel ***
                                onBackClick = { navController.popBackStack() }, // 点击返回，回到 POI 详情页
                                onNavigateStart = {
                                    navController.navigate(NavigationRoute)
                                }
                            )
                        }

                        // *** 导航界面 ***
                        composable(NavigationRoute) {
                            NavigationScreen(
                                routePlanViewModel = routePlanViewModel,
                                onEndNaviTask = {
                                    routePlanViewModel.onCameraDialogDismissed() // 先隐藏对话框
                                    navController.navigate(CameraRecognitionRoute)
                                }
                            )
                        }

                        // ***识别界面 ***
                        composable(CameraRecognitionRoute) {
                            CameraRecognitionScreen(
                                routePlanViewModel = routePlanViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    // 检查前台权限是否全部授予
    private fun hasAllForegroundPermissions(): Boolean {
        return foregroundPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 检查后台定位权限是否授予
    private fun hasBackgroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 检查是否需要请求后台定位权限
    private fun requestBackgroundLocationIfNeeded() {
        // Android 10+ 才有后台定位权限
        if (!hasBackgroundLocationPermission() && !hasRequestedBackgroundLocation && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            hasRequestedBackgroundLocation = true
            Log.d("PERMISSION", "请求后台定位权限")
            requestBackgroundLocationLauncher.launch(backgroundPermissions)
        }
    }

}