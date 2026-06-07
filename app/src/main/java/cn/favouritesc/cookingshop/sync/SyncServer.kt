package cn.favouritesc.cookingshop.sync

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CopyOnWriteArrayList

private const val TAG = "SyncServer"

typealias SnapshotProvider = suspend () -> JSONObject
typealias WriteHandler = suspend (JSONObject) -> JSONObject

/** 内嵌 HTTP 服务端（主机端），客户端定期 GET /poll?since=timestamp 拉取增量 */
class SyncServer(
    private val port: Int,
    private val snapshotProvider: SnapshotProvider? = null,
    private val writeHandler: WriteHandler? = null,
    private val filesDir: File? = null
) : NanoHTTPD(port) {

    // 变更日志（主机写，客户端拉）
    private val changelog = CopyOnWriteArrayList<SyncMessage>()
    private var clientCount = 0

    fun startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.d(TAG, "Server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
        }
    }

    /** 记录一条变更 */
    fun append(msg: SyncMessage) {
        changelog.add(msg)
        // 保留最新 200 条
        while (changelog.size > 200) changelog.removeAt(0)
    }

    /** 从 since 时间戳（取 log 索引）之后的所有变更 */
    private fun getChanges(since: Int): List<SyncMessage> {
        if (since < 0 || since >= changelog.size) return emptyList()
        return changelog.subList(since, changelog.size)
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        val method = session.method
        return when {
            // POST /write: 客户端写操作提交到主机
            method == Method.POST && uri == "/write" -> handleWrite(session)
            uri == "/poll" -> {
                val since = session.parameters["since"]?.firstOrNull()?.toIntOrNull() ?: 0
                val changes = getChanges(since)
                val json = JSONObject().apply {
                    put("cursor", changelog.size)
                    put("changes", JSONArray().apply { changes.forEach { put(JSONObject(it.toJson())) } })
                }
                newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
            }
            uri == "/hello" -> {
                clientCount++
                Log.d(TAG, "Client hello, total=$clientCount")
                val json = JSONObject().apply { put("cursor", changelog.size) }
                newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
            }
            uri == "/full" -> {
                if (snapshotProvider == null) {
                    newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, "text/plain", "snapshot not available")
                } else {
                    try {
                        val snapshot = runBlocking { snapshotProvider() }
                        snapshot.put("cursor", changelog.size)
                        newFixedLengthResponse(Response.Status.OK, "application/json", snapshot.toString())
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to build snapshot", e)
                        newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "snapshot error")
                    }
                }
            }
            uri.startsWith("/image/") -> handleImage(uri.removePrefix("/image/"))
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
        }
    }

    private fun handleWrite(session: IHTTPSession): Response {
        if (writeHandler == null) {
            return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, "text/plain", "write not available")
        }
        return try {
            val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
            val bytes = ByteArray(contentLength)
            session.inputStream.read(bytes)
            val body = JSONObject(String(bytes))
            val result = runBlocking { writeHandler(body) }
            newFixedLengthResponse(Response.Status.OK, "application/json", result.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Write failed", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                JSONObject().apply { put("error", e.message ?: "unknown") }.toString())
        }
    }

    private fun handleImage(encodedPath: String): Response {
        if (filesDir == null) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "no filesDir")
        }
        return try {
            val decoded = java.net.URLDecoder.decode(encodedPath, "UTF-8")
            val file = File(decoded)
            // 安全检查：只允许从 filesDir 目录读取
            if (!file.canonicalPath.startsWith(filesDir.canonicalPath)) {
                Log.w(TAG, "Image access denied: $decoded")
                return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "forbidden")
            }
            if (!file.exists() || !file.isFile) {
                Log.w(TAG, "Image not found: $decoded")
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
            }
            val mime = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            }
            newFixedLengthResponse(Response.Status.OK, mime, file.inputStream(), file.length())
        } catch (e: Exception) {
            Log.e(TAG, "Image serve failed", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error")
        }
    }
}
