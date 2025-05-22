package world.accera.dawn.utils

import com.google.mediapipe.tasks.genai.llminference.LlmInference

// 定义 Gemma 模型加载的各种状态
sealed class ModelStateUtils {
    data object NotInitialized : ModelStateUtils()                 // 未初始化
    data object Initializing : ModelStateUtils()                   // 初始化中
    data class Initialized(val llmInference: LlmInference) : ModelStateUtils() // 初始化成功，并持有实例
    data class Error(val message: String) : ModelStateUtils() // 初始化失败
}