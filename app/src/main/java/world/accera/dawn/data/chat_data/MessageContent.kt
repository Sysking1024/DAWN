package world.accera.dawn.data.chat_data

import java.util.UUID

data class MessageContent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val role: Role,
    val type: MessageType,
    val messageContent: String
)