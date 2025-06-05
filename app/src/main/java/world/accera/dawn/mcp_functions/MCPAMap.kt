package world.accera.dawn.mcp_functions

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.modelcontextprotocol.kotlin.sdk.ListResourceTemplatesRequest
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.coroutines.CoroutineScope

object MCPAMap : MCP() {

    private const val AMAP_MCP_SERVER_URL = "https://mcp.amap.com"
    private const val AMAP_MCP_SERVER_KEY = "4f825fa3e082a6f92fe7cf560c7728fd"

        override val transport: Transport
        get() = SseClientTransport(
            HttpClient {
                install(SSE)
            },
            urlString = AMAP_MCP_SERVER_URL,
            requestBuilder = {
                header("key", AMAP_MCP_SERVER_KEY)
            }
        )
    override val listResourceTemplatesRequest: ListResourceTemplatesRequest
        get() = ListResourceTemplatesRequest(null)

    fun initialize(coroutineScope: CoroutineScope) {
        internalInitialize(coroutineScope)
    }

    fun destroy(coroutineScope: CoroutineScope) {
        internalDestroy(coroutineScope)
    }

}