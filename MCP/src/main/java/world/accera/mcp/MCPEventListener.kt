
package world.accera.mcp

interface MCPEventListener {
    fun onEvent(data: String)
    fun onError(error: Throwable)
}
