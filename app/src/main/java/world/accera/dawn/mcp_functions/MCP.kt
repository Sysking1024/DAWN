package world.accera.dawn.mcp_functions

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListPromptsResult
import io.modelcontextprotocol.kotlin.sdk.ListResourceTemplatesRequest
import io.modelcontextprotocol.kotlin.sdk.ListResourceTemplatesResult
import io.modelcontextprotocol.kotlin.sdk.ListResourcesResult
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class MCP {

    companion object {
        val clientInfo = Implementation(
            name = "DAWN",
            version = "0.1.0"
        )

        var client: Client? = null
    }

    var listToolsResult: ListToolsResult? = null
    var listResourcesResult: ListResourcesResult? = null
    var listPromptsResult: ListPromptsResult? = null
    var listResourceTemplatesResult: ListResourceTemplatesResult? = null

    abstract val transport: Transport
    abstract val listResourceTemplatesRequest: ListResourceTemplatesRequest

    fun internalInitialize(coroutineScope: CoroutineScope) {
        client = Client(clientInfo)
        coroutineScope.launch(Dispatchers.Default) {
            client?.connect(transport)
            listToolsResult = client?.listTools()
            //listResourcesResult = client?.listResources()
            //listPromptsResult = client?.listPrompts()
            //listResourceTemplatesResult = client?.listResourceTemplates(listResourceTemplatesRequest)
        }
    }

    fun internalDestroy(coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Default) {
            client?.close()
        }
        client = null
        listToolsResult = null
        listPromptsResult = null
        listResourcesResult = null
        listResourceTemplatesResult = null
    }
}