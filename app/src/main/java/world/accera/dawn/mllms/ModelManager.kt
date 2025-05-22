package world.accera.dawn.mllms

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions.*
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import world.accera.dawn.utils.ModelStateUtils

object ModelManager {
    private const val TAG = "GemmaModelManager"
    private const val MODEL_PATH = "/data/local/tmp/llm/model_version.task"

    private var llmInference: LlmInference? = null
    private var session: LlmInferenceSession? = null
    private var options: LlmInferenceOptions? = null
    private var sessionOptions: LlmInferenceSessionOptions? = null

    private var initializationJob: Job? = null

    // 使用 MutableStateFlow 来管理和广播初始化状态
    private val _initState = MutableStateFlow<ModelStateUtils>(ModelStateUtils.NotInitialized)
    val initState  = _initState.asStateFlow() // 对外暴露为不可变的 StateFlow

    /**
     * 异步初始化 Gemma 模型。
     * 此方法应该在 Application 的 onCreate 中被调用。
     * 使用 CoroutineScope 来确保即使调用者作用域结束，初始化也能继续（如果需要）。
     * @param applicationContext 应用上下文
     * @param coroutineScope 用于执行初始化任务的协程作用域
     */
    fun initializeAsync(applicationContext: Context, coroutineScope: CoroutineScope) {
if (_initState.value is ModelStateUtils.Initializing || _initState.value is ModelStateUtils.Initialized) {
    Log.d(TAG, "模型已在初始化或已初始化完成。")
    return
}

        _initState.value = ModelStateUtils.Initializing
        Log.d(TAG, "开始异步初始化 Gemma 模型...")

        // 初始化 MediaPipe LlmInference
        initializationJob = coroutineScope.launch(Dispatchers.IO) { // IO 线程适合文件操作和CPU密集型任务
            options =
                builder()
                .setModelPath(MODEL_PATH)
                .setMaxNumImages(1)
                .setPreferredBackend(LlmInference.Backend.GPU)
                .build()

            sessionOptions =
                LlmInferenceSessionOptions.builder()
                    .setTopK(10)
                    .setTemperature(0.2f)
                    .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                    .build()

            try {
                val inferenceInstance =
                    LlmInference.createFromOptions(applicationContext, options)
                 llmInference = inferenceInstance
                session =
                    LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
                _initState.value = ModelStateUtils.Initialized(inferenceInstance) // 更新状态为已初始化
                        Log.i(TAG, "模型初始化成功！")
            } catch (e: Exception) {
                Log.e(TAG, "Gemma 模型初始化失败。", e)
                _initState.value = ModelStateUtils.Error("模型初始化失败: ${e.message}")
            }
        }
    }

    /**
     * 异步识别图片并生成描述。
     * @param bitmap 需要识别的图片。
     * @param prompt 提示词，可留空。默认是“用中文描述图片”
     * @return String? 如果成功生成描述则返回描述文本，否则返回 null。
     */
    suspend fun recognition(bitmap: Bitmap, prompt: String = "用中文描述图片"): String? {
        // 检查初始化状态，这一步不需要切换上下文，因为它很快
        if (_initState.value !is ModelStateUtils.Initialized) {
            Log.w(TAG, "模型未初始化，无法执行识别。")
            return null
        }

        // 切换到 Default 调度器执行 CPU 密集型任务
        return withContext(Dispatchers.Default) {
            Log.d(TAG, "在线程 ${Thread.currentThread().name} 上开始执行识别...")
            try {
                // 转换图片并识别
                val mpImage = BitmapImageBuilder(bitmap).build()
                // 确保 session 是线程安全的，或者这里的调用是预期的。
                // LlmInferenceSession 的方法通常是阻塞的，
                // 所以将它们放在 withContext 块中是正确的。
                session?.addQueryChunk(prompt)
                session?.addImage(mpImage)
                val result = session?.generateResponse()
                Log.d(TAG, "识别完成。")
                result // 返回结果
            } catch (e: Exception) {
                Log.e(TAG, "执行识别时发生错误。", e)
                null // 发生错误时返回 null
            }
        }
    }

    fun clearSession() {
        this.session = null // 清理session，防止令牌过长
        session =
            LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
    }

    /**
     * 关闭 LlmInference 实例并重置状态。
     * 应该在 Application 的 onTerminate（如果合适）或不再需要模型时调用。
     */
    fun release() {
        Log.d(TAG, "正在释放 ModelManager 资源...")
        initializationJob?.cancel() // 取消正在进行的初始化（如果存在）
        initializationJob = null
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(TAG, "关闭 LlmInference 实例时出错", e)
        }
        llmInference = null
        _initState.value = ModelStateUtils.NotInitialized
        Log.d(TAG, "GemmaModelManager 资源已释放。")
    }
}