package cn.favouritesc.cookingshop.sync

import org.json.JSONArray
import org.json.JSONObject

/** 消息类型 */
enum class MsgType {
    // 全量同步
    SYNC_FULL,
    // 增量变更
    DISH_SAVED, DISH_DELETED,
    ORDER_SAVED, ORDER_DELETED,
    INGREDIENT_SAVED, INGREDIENT_DELETED,
    // 设备信息
    DEVICE_JOIN, DEVICE_LEAVE
}

/** 一条同步消息 */
data class SyncMessage(
    val type: MsgType,
    val payload: JSONObject? = null
) {
    fun toJson(): String = JSONObject().apply {
        put("type", type.name)
        payload?.let { put("payload", it) }
    }.toString()

    companion object {
        fun fromJson(json: String): SyncMessage {
            val obj = JSONObject(json)
            return SyncMessage(
                type = MsgType.valueOf(obj.getString("type")),
                payload = obj.optJSONObject("payload")
            )
        }
    }
}

/** 积压帧重组器 — 处理 WebSocket 分帧到达 */
class MessageFramer {
    private val buffer = StringBuilder()

    fun feed(chunk: String): SyncMessage? {
        buffer.append(chunk)
        val firstBrace = buffer.indexOf('{')
        if (firstBrace < 0) return null // 还没收到开始
        if (firstBrace > 0) buffer.delete(0, firstBrace) // 去掉头垃圾

        // 简单括号计数找完整帧
        var depth = 0
        for (i in buffer.indices) {
            when (buffer[i]) {
                '{' -> depth++
                '}' -> depth--
            }
            if (depth == 0 && i > 0) {
                val frame = buffer.substring(0, i + 1)
                buffer.delete(0, i + 1)
                return SyncMessage.fromJson(frame)
            }
        }
        return null
    }
}
