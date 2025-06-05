
package world.accera.mcp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class MCPClient(
    private val url: String,
    private val apiKey: String,
    private val listener: MCPEventListener
) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val transport = MCPTransport(url, apiKey, listener)

    fun connect() {
        transport.connect()
    }

    suspend fun sendMessage(id: String, method: String, params: JsonElement) {
        val request = JSONRPCRequest(id, method, params)
        val jsonBody = Json.encodeToString(request)

        try {
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                headers {
                    append("Authorization", "Bearer \$apiKey")
                }
                setBody(jsonBody)
            }

            val result = response.bodyAsText()
            println("MCP Response: \$result")
        } catch (e: Exception) {
            listener.onError(e)
        }
    }

    fun disconnect() {
        transport.disconnect()
    }
}
