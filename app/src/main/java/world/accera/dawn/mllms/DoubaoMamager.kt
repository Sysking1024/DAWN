package world.accera.dawn.mllms

import android.graphics.Bitmap
import android.util.Base64
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionContentPart
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole
import com.volcengine.ark.runtime.service.ArkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object DoubaoMamager {

    private const val DOUBAO_API_KEY = "f368b910-7cc3-4979-a1cf-e79517d1a4af"

    private var arkService: ArkService? = null
    private var chatMessages: MutableList<ChatMessage>? = null
    private var chatCompletionRequest: ChatCompletionRequest? = null
    private val models = mutableListOf("doubao-1-5-thinking-vision-pro-250428", "doubao-1.5-vision-pro-250328")

    fun initialize(systemInstruction: String = "描述图片") {
arkService = ArkService.builder()
    .apiKey(DOUBAO_API_KEY)
    .build()

        val systemPrompt = ChatMessage.builder()
            .role(ChatMessageRole.SYSTEM)
            .content(systemInstruction)
            .build()
        chatMessages = mutableListOf(systemPrompt)

        chatCompletionRequest = ChatCompletionRequest.builder()
            .model(models[0])
            .messages(chatMessages)
            .build()
    }

    suspend fun recognition(bitmap: Bitmap, prompt: String = "描述图片"): String = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        val imgBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        val imgPart = ChatCompletionContentPart.builder()
            .type("image_url")
            .imageUrl(ChatCompletionContentPart.ChatCompletionContentPartImageURL(
                "data:image/jpeg;base64,$imgBase64", "low"
            )).build()
        val txtPart = ChatCompletionContentPart.builder()
            .type("text")
            .text(prompt)
            .build()

        val userMessage = ChatMessage.builder()
            .role(ChatMessageRole.USER)
            .multiContent(
                mutableListOf(txtPart, imgPart)
            )
            .build()

        chatMessages?.add(userMessage)

        var result = ""
        arkService?.createChatCompletion(chatCompletionRequest)?.choices
            ?.forEach { choice: ChatCompletionChoice ->
                result = choice.message.content.toString()
            }

        result
    }

    fun destroy() {
        arkService?.shutdownExecutor()
    }
}