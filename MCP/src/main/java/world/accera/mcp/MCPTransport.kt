
package world.accera.mcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader

class MCPTransport(
    private val url: String,
    private val apiKey: String,
    private val listener: MCPEventListener
) {
    private val client = OkHttpClient()
    private var running = false

    fun connect() {
        running = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("\$url?key=\$apiKey")
                    .addHeader("Accept", "text/event-stream")
                    .build()

                val response = client.newCall(request).execute()
                val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                var line: String?
                val buffer = StringBuilder()

                while (running && reader.readLine().also { line = it } != null) {
                    line?.let {
                        if (it.startsWith("data:")) {
                            buffer.append(it.removePrefix("data:"))
                        } else if (it.isEmpty()) {
                            listener.onEvent(buffer.toString())
                            buffer.clear()
                        }
                    }
                }
            } catch (e: Exception) {
                listener.onError(e)
            }
        }
    }

    fun disconnect() {
        running = false
    }
}
