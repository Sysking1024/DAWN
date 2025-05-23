package world.accera.dawn

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import world.accera.dawn.mllms.ModelManager
import java.util.concurrent.Executor
import androidx.core.graphics.scale

private const val TAG = "CameraRecognitionScreen"
private const val TARGET_IMAGE_RESOLUTION = 768 // 可以选择 256, 512, 或 768

@Composable
fun CameraRecognitionScreen(
    routePlanViewModel: RoutePlanViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope() // 获取协程作用域
    var recognitionResult by remember { mutableStateOf<String?>(null) }
    var isLoadingRecognition by remember { mutableStateOf(false) }
    val currentView = LocalView.current
    val destinationName by routePlanViewModel.destinationNameState
    val prompt = "你是一个有着丰富经验的协助视障者出行的志愿者。现在有一位视障用户通过导航软件提供的连线功能与你取得了联系，他需要你告诉他目的地是否在图片中。你正在通过视障用户的手机摄像头帮助他。你需要用最精简、最准确的语言描述目的地是否在图片中，如果在图片中的话，你需要告诉用户方位。为了让你理解，下面以目的地是“肯德基”为例，来说明你要回复的句式。你需要严格遵守句式回复用户。例子：假设目的地是“肯德基”，如果目的地不在图片中：“我没有看到肯德基，你可能需要调整下方向重新拍摄照片”；如果图片中有目的地：“我看到肯德基了，大约在你10点方向，20米的样子。”。下面是用户要去的目的地，你需要灵活替换例子中的“肯德基”和方位词：“${ destinationName }”"

    // 设置相机控制器
    LaunchedEffect(Unit) {
        cameraController.bindToLifecycle(lifecycleOwner)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        // 如果需要，您还可以设置其他参数，如图像分析、视频捕获等
        // 目前，我们只需要预览和图像捕获。
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = cameraController
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // "识别" 按钮
        Button(
            onClick = {
                capturePhoto(
                    context = context,
                    cameraController = cameraController,
                    onPhotoCaptured = { bitmap ->
                        capturedBitmap = bitmap
                        Log.i(TAG, "照片已捕捉。Bitmap 尺寸: ${bitmap.width}x${bitmap.height}")

                        recognitionResult = null // 清除旧结果
                        scope.launch {
                            val result = ModelManager.recognition(bitmap, prompt)
                            //ModelManager.clearSession()
                            recognitionResult = result ?: "未能获取描述。"
                            currentView.announceForAccessibility(result)
                        }
                    },
                    onError = { exception ->
                        Log.e(TAG, "捕捉照片时出错: ${exception.message}", exception)
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp) // 增加了底部的内边距
                .size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.7f))
        ) {
            // 您可以为按钮使用图标或文本
            // 根据您的描述 "识别"，这里使用文本
            Text("识别", fontSize = 18.sp, color = Color.Black)
            // 使用图标的示例:
            // Icon(Icons.Filled.Camera, contentDescription = "识别", tint = Color.Black, modifier = Modifier.size(36.dp))
        }

        // --- 可选: 用于调试，显示捕捉到的 bitmap ---
        // capturedBitmap?.let { bmp ->
        //     Image(
        //         bitmap = bmp.asImageBitmap(),
        //         contentDescription = "捕捉到的照片",
        //         modifier = Modifier
        //             .size(150.dp)
        //             .align(Alignment.TopStart)
        //             .padding(16.dp)
        //             .border(2.dp, Color.Red)
        //     )
        // }
        // --- 可选结束 ---
    }
}

private fun capturePhoto(
    context: Context,
    cameraController: LifecycleCameraController,
    onPhotoCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    cameraController.takePicture(
        mainExecutor, // 将回调调度到主线程以进行 UI 更新
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                Log.i(TAG, "捕捉成功。图像旋转角度: ${imageProxy.imageInfo.rotationDegrees}")
                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                }
                val imagePlanes = imageProxy.planes
                if (imagePlanes.isNotEmpty()) {
                    val buffer = imagePlanes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    // 如果需要进一步处理，确保 bitmap 是可变格式，
                    // 尽管 BitmapFactory 通常返回可变 bitmap。
                    // 对于旋转，createBitmap 会处理这个问题。
                    val rotatedBitmap = Bitmap.createBitmap(
                        originalBitmap,
                        0,
                        0,
                        originalBitmap.width,
                        originalBitmap.height,
                        matrix,
                        true // filter: true 表示如果缩放则质量更好
                    )

                    val resizedBitmap = rotatedBitmap.scale(TARGET_IMAGE_RESOLUTION, TARGET_IMAGE_RESOLUTION)
                    Log.i(TAG, "调整尺寸后 Bitmap 尺寸: ${resizedBitmap.width}x${resizedBitmap.height}")
                    onPhotoCaptured(resizedBitmap)
                } else {
                    onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "图像平面为空", null))
                }
                imageProxy.close() // 至关重要：关闭 ImageProxy 以释放资源
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "图像捕捉错误", exception)
                onError(exception)
            }
        }
    )
}