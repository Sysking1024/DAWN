package world.accera.dawn.mllms

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiManager {

    private const val TAG = "GeminiManager"

    private var geminiModel: GenerativeModel? = null
    private var chat: Chat? = null

    fun initialize(systemInstruction: String) {
if (geminiModel != null) {
    return
}

        val config = generationConfig {
            maxOutputTokens = 100
            temperature = 0.4f
            topK = 10
            topP = 0.5f
            frequencyPenalty = 1.5f
        }

    geminiModel = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.0-flash",
                generationConfig = config,
                systemInstruction = content { text(systemInstruction) }
            )
        chat = geminiModel!!.startChat()
    }

    suspend fun recognition(bitmap: Bitmap, prompt: String = "用中文描述图片"): String? {
        val response = withContext(Dispatchers.Default) {
            chat?.sendMessage(
                content {
                    text(prompt)
                    image(bitmap)
                }
            )
        }

        return response?.text
    }
}