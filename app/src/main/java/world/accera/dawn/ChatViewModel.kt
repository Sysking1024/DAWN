package world.accera.dawn

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import world.accera.dawn.data.chat_data.MessageContent
import world.accera.dawn.data.chat_data.MessageType
import world.accera.dawn.data.chat_data.Role

class ChatViewModel : ViewModel() {

    private val _messages = mutableStateListOf<MessageContent>()
    val messages: List<MessageContent> = _messages

    var inputText by mutableStateOf("")
        private set
    var isVoiceInputMode by mutableStateOf(true)
        private set
    var isFunctionPanelExpanded by mutableStateOf(false)
        private set

    init {
        _messages.add(MessageContent(
            messageContent = "你好，我是晓向AI助手，有什么可以帮你的吗？",
            type = MessageType.TEXT,
            role = Role.DAWN_AI
        ))
    }

    fun updateInputText(newText: String) {
        inputText = newText
    }

    fun sendTextMessage() {
        if (!inputText.isNotBlank()) {
            return
        }

        // 发送消息
        _messages.add(MessageContent(
            messageContent = inputText,
            type = MessageType.TEXT,
            role = Role.USER
        ))

        viewModelScope.launch {
            // 模拟回复
            kotlinx.coroutines.delay(1000)
            _messages.add(MessageContent(
                messageContent = "我已经收到你的消息了: \"$inputText\"",
                type = MessageType.TEXT,
                role = Role.DAWN_AI
            ))
        }
        inputText = ""
    }

    fun sendVoiceMessage(voiceData: String) {
        // 模拟语音消息
        _messages.add(MessageContent(
            messageContent = voiceData,
            type = MessageType.AUDIO,
            role = Role.USER
        ))
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _messages.add(MessageContent(
                messageContent = "我听到了你的语音。",
                type = MessageType.TEXT,
                role = Role.DAWN_AI
            ))
        }
    }

    fun toggleInputMode() {
        Log.d("ChatViewModel", "点击了文本语音切换按钮，isVoiceMode值改变之前: $isVoiceInputMode")
        isVoiceInputMode = !isVoiceInputMode
        Log.d("ChatViewModel", "点击了文本语音切换按钮，isVoiceMode值改变之后: $isVoiceInputMode")
        if (isVoiceInputMode) {
            isFunctionPanelExpanded = false
        }
    }

    fun toggleFunctionPanel() {
        isFunctionPanelExpanded = !isFunctionPanelExpanded
    }

    fun onPersonalCenterClick() {
        TODO("左侧边导航占位")
    }
}