package cn.favouritesc.cookingshop.sync

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "SyncClient"
private const val POLL_INTERVAL_MS = 2000L
private const val MAX_CONSECUTIVE_FAILURES = 3

enum class ClientState { DISCONNECTED, CONNECTING, CONNECTED }

class SyncClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cursor = 0
    private var pollingJob: Job? = null

    private val _state = MutableStateFlow(ClientState.DISCONNECTED)
    val state: StateFlow<ClientState> = _state

    var onMessage: ((SyncMessage) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    fun connect(hostIp: String, port: Int) {
        if (_state.value != ClientState.DISCONNECTED) return
        _state.value = ClientState.CONNECTING
        scope.launch {
            try {
                // 首次握手获取起始 cursor
                val helloReq = Request.Builder().url("http://$hostIp:$port/hello").build()
                val helloResp = client.newCall(helloReq).execute()
                if (helloResp.isSuccessful) {
                    val body = helloResp.body?.string() ?: ""
                    cursor = JSONObject(body).optInt("cursor", 0)
                    _state.value = ClientState.CONNECTED
                    Log.d(TAG, "Connected to $hostIp:$port, cursor=$cursor")
                    startPolling(hostIp, port)
                } else {
                    _state.value = ClientState.DISCONNECTED
                    retry(hostIp, port)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Connection failed", e)
                _state.value = ClientState.DISCONNECTED
                retry(hostIp, port)
            }
        }
    }

    private fun retry(hostIp: String, port: Int) {
        scope.launch { delay(5000); if (_state.value == ClientState.DISCONNECTED) connect(hostIp, port) }
    }

    private fun startPolling(hostIp: String, port: Int) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            var consecutiveFailures = 0
            while (isActive && _state.value == ClientState.CONNECTED) {
                delay(POLL_INTERVAL_MS)
                try {
                    val req = Request.Builder().url("http://$hostIp:$port/poll?since=$cursor").build()
                    val resp = client.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val obj = JSONObject(body)
                        val newCursor = obj.optInt("cursor", cursor)
                        val changes = obj.optJSONArray("changes") ?: JSONArray()
                        for (i in 0 until changes.length()) {
                            val msgJson = changes.getJSONObject(i).toString()
                            SyncMessage.fromJson(msgJson).let { msg ->
                                Log.d(TAG, "Poll received: ${msg.type}")
                                onMessage?.invoke(msg)
                            }
                        }
                        cursor = newCursor
                        consecutiveFailures = 0
                    } else {
                        consecutiveFailures++
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) break
                    }
                } catch (e: Exception) {
                    consecutiveFailures++
                    Log.w(TAG, "Poll failed ($consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)", e)
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) break
                }
            }
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                _state.value = ClientState.DISCONNECTED
                onDisconnected?.invoke()
            }
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        _state.value = ClientState.DISCONNECTED
        Log.d(TAG, "Disconnected")
    }

    /** 拉取主机全量快照（客户端首次连接时调用） */
    suspend fun fetchFull(hostIp: String, port: Int): JSONObject = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("http://$hostIp:$port/full").build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw RuntimeException("/full returned ${resp.code}")
        JSONObject(resp.body?.string() ?: "{}")
    }

    /** 向主机提交写操作 */
    suspend fun postWrite(hostIp: String, port: Int, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val reqBody = body.toString().toRequestBody(mediaType)
        val req = Request.Builder().url("http://$hostIp:$port/write").post(reqBody).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw RuntimeException("/write returned ${resp.code}")
        JSONObject(resp.body?.string() ?: "{}")
    }
}
