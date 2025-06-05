/**
 * 导航结束之后的拍摄识别界面
 * 截止6.5的主要功能是，导航结束选择AI寻找目的地后，会跳转该界面。
 * 点击识别按钮后，会拍摄照片，随后将照片和prompt推送到模型进行推理。
 */

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
import androidx.core.graphics.scale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import world.accera.dawn.mllms.DoubaoMamager
import java.util.concurrent.Executor

private const val TAG = "CameraRecognitionScreen"
private const val TARGET_IMAGE_RESOLUTION = 768 // 可以选择 256, 512, 或 768
private const val NEXT_PROMPT = "这张呢？"

private const val IMAGE_TOKEN = 256

private var tokensSize = 0

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
    val prompt = """
你是一位有着服务视障者丰富经验的志愿者。
现在有一位视障用户，通过导航软件与你联系，他已到达导航终点附近，希望你能帮他找到“${destinationName}”的确切入口。
他会向你发送他面前环境的照片。

你的任务是：
1.  仔细分析图像，判断目标“${destinationName}”是否清晰可见。
2.  如果目标有明显的文字标识，请直接确认。
3.  如果目标没有明显文字标识，请尝试根据建筑物的特征（如颜色、形状、材质）、周围环境的标志性物体（如旁边的店铺、特殊树木、雕塑等）或与“${destinationName}”通常相关的视觉线索来推断其是否在照片中。
4.  用最简洁、准确、口语化的语言回答他：
    * 如果确认看到目标：明确指出目标位置（例如，几点钟方向、参照物旁边）和大致距离。
    * 如果无法在照片中找到目标：清晰说明未看到，并根据照片中实际看到的景物，给出调整拍摄方向或位置的具体建议，帮助他进行下一步尝试。
    * 如果你根据环境推断目标可能在照片中，但无法完全确认（例如，招牌文字或标志被部分遮挡或特征相似）：请表达你的推断和不确定性，并指导他如何获取更清晰的视角。

回应示例：
* （清晰可见）：“我看到‘${destinationName}’了，就在你前面大约11点钟方向，看起来不远，也就十几米。”
* （未看到，但提供指导）：“照片里暂时没看到‘${destinationName}’。我看到你正对着一个XXX（描述你从图片中看到的环境特征）。你可以试试向你的右手边转，再拍一张照片给我看看。”
* （推断但需确认）：“我看到XXX（描述你从图片中看到的环境特征），这看起来有点像‘${destinationName}’的描述，但没有看到招牌。它在你大概1点钟方向。你能稍微走近一点，或者调整下角度，让我看得更清楚些吗？”

绝对禁止出现：
* 禁止用“你能看到”短语：用户是视障者，他无法看到任何东西，所以在你的回答中不可以出现“你能看到”或“你看到”等类似短语，这不尊重用户。
* 绝对不可以无中生有：你的中级目标是确保准确服务用户，如果确实不好判断，那必须坦诚告知用户。但非常鼓励你对有把握的推测进行大胆决策。

请直接给出你的判断和具体建议，保持友好和耐心。回答字数不超过50字。
""".trimIndent()

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
                            //val inputText = if (tokensSize == 0) prompt else NEXT_PROMPT
                            val inputText = "这张照片有${destinationName}吗？"
                            //val result = GemmaManager.recognition(bitmap, inputText)
                            //val result = GeminiManager.recognition(bitmap, inputText)
                            val result = DoubaoMamager.recognition(bitmap, inputText)
                            recognitionResult = result
                            currentView.announceForAccessibility(result)

                            // 计算Tokens，如果超过限制，则重置session
                            /*tokensSize += IMAGE_TOKEN + (GemmaManager.sizeInTokens(inputText + result) ?: 0)
                            if (tokensSize >= 3500 || result.isNullOrBlank()) {
                                GemmaManager.newSession()
                                tokensSize = 0
                            }
                            currentView.announceForAccessibility("token = $tokensSize")*/
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

                    val resizedBitmap =
                        rotatedBitmap.scale(TARGET_IMAGE_RESOLUTION, TARGET_IMAGE_RESOLUTION)
                    Log.i(
                        TAG,
                        "调整尺寸后 Bitmap 尺寸: ${resizedBitmap.width}x${resizedBitmap.height}"
                    )
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