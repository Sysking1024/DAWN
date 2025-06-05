
package world.accera.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class JSONRPCRequest(
    val id: String,
    val method: String,
    val params: JsonElement,
    val jsonrpc: String = "2.0"
)

@Serializable
data class JSONRPCResponse(
    val id: String,
    val result: JsonElement?,
    val error: JsonElement?,
    val jsonrpc: String = "2.0"
)
