package cn.favouritesc.cookingshop.sync

import android.content.Context
import android.util.Log
import cn.favouritesc.cookingshop.data.db.DatabaseHelper
import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.DishIngredient
import cn.favouritesc.cookingshop.data.db.DishTagCrossRef
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.IngredientCategory
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.data.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface

private const val TAG = "SyncManager"

enum class SyncRole { NONE, HOST, CLIENT }
data class SyncStatus(val role: SyncRole, val clientState: ClientState? = null, val clientCount: Int = 0)

class SyncManager(
    private val dishRepository: DishRepository,
    private val orderRepository: OrderRepository,
    private val ingredientRepository: IngredientRepository,
    private val databaseHelper: DatabaseHelper,
    context: Context
) {
    val nsdHelper = NsdHelper(context)

    private var server: SyncServer? = null
    private var client: SyncClient? = null
    private var syncConsumerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 串行广播队列：保证主机端消息按 FIFO 顺序写入 changelog
    private val writeChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (action in writeChannel) { action() }
        }
    }

    private val _status = MutableStateFlow(SyncStatus(SyncRole.NONE))
    val status: StateFlow<SyncStatus> = _status

    private val _hostIp = MutableStateFlow("")
    val hostIp: StateFlow<String> = _hostIp

    private val _hostPort = MutableStateFlow(8765)
    val hostPort: StateFlow<Int> = _hostPort

    private val _syncEvents = MutableSharedFlow<SyncMessage>(replay = 0, extraBufferCapacity = 64)
    val syncEvents: SharedFlow<SyncMessage> = _syncEvents

    private val _dataVersion = MutableStateFlow(0L)
    /** 每次同步事件处理完成后递增，供 UI 层触发一次性查询刷新 */
    val dataVersion: StateFlow<Long> = _dataVersion

    // === 主机模式 ===

    fun startHost(port: Int = 8765) {
        _hostPort.value = port
        _hostIp.value = getLocalIpAddress()
        server = SyncServer(
            port,
            snapshotProvider = { buildFullSnapshot() },
            writeHandler = { json -> handleWrite(json) }
        ).apply {
            startServer()
        }
        // 注册 NSD 服务，供客户端自动发现
        nsdHelper.registerService(port)
        _status.value = SyncStatus(SyncRole.HOST, clientCount = 0)
        Log.d(TAG, "Host mode started on port $port")
    }

    fun stopHost() {
        nsdHelper.unregisterService()
        server?.stop()
        server = null
        _status.value = SyncStatus(SyncRole.NONE)
        Log.d(TAG, "Host mode stopped")
    }

    // === 客户端模式 ===

    suspend fun joinHost(ip: String, port: Int = 8765) {
        Log.d(TAG, "joinHost: starting connection to $ip:$port")
        _hostIp.value = ip
        _hostPort.value = port
        // ① 断开旧 + 等待当前 import 完成
        Log.d(TAG, "joinHost: step 1 — disconnect old client")
        client?.disconnect()
        client = null
        syncConsumerJob?.cancel()
        syncConsumerJob?.join()
        syncConsumerJob = null
        // ② DB 切换到主机模式（IO 线程）
        Log.d(TAG, "joinHost: step 2 — switchToHostMode")
        try {
            withContext(Dispatchers.IO) {
                databaseHelper.switchToHostMode()
            }
        } catch (e: Exception) {
            Log.e(TAG, "joinHost: switchToHostMode failed", e)
            _status.value = SyncStatus(SyncRole.NONE)
            return  // 不回滚，本地 DB 仍可用
        }
        // ③ 连接主机
        Log.d(TAG, "joinHost: step 3 — connect to host")
        val newClient = SyncClient().apply {
            onMessage = { msg ->
                Log.d(TAG, "Client got sync: ${msg.type} payloadKeys=${msg.payload?.keys()?.asSequence()?.toList()}")
                _syncEvents.tryEmit(msg)
            }
            onDisconnected = {
                Log.w(TAG, "Host disconnected, auto-recovering")
                scope.launch { leaveHost() }
            }
        }
        client = newClient
        scope.launch {
            newClient.state.collect { clientState ->
                Log.d(TAG, "Client state changed: $clientState")
                _status.value = _status.value.copy(clientState = clientState)
            }
        }
        newClient.connect(ip, port)
        // ④ 拉取全量快照并导入
        Log.d(TAG, "joinHost: step 4 — fetch full snapshot")
        try {
            val fullJson = newClient.fetchFull(ip, port)
            Log.d(TAG, "joinHost: snapshot received, keys=${fullJson.keys().asSequence().toList()}")
            importFullSnapshot(fullJson)
            Log.d(TAG, "joinHost: snapshot imported successfully")
        } catch (e: Exception) {
            Log.e(TAG, "joinHost: import full snapshot failed — rolling back to local DB", e)
            try { databaseHelper.restoreLocalMode() } catch (_: Exception) {}
            databaseHelper.emitDataChange()
            newClient.disconnect()
            client = null
            _status.value = SyncStatus(SyncRole.NONE)
            return  // ← 不再 throw，避免闪退
        }
        databaseHelper.emitDataChange()
        // ⑤ 启动增量消费（内部有 try/catch 保护）
        Log.d(TAG, "joinHost: step 5 — start sync consumer")
        syncConsumerJob = scope.launch {
            _syncEvents.collect { msg -> handleSyncEvent(msg) }
        }
        _dataVersion.value++
        _status.value = SyncStatus(SyncRole.CLIENT, ClientState.CONNECTED)
        Log.d(TAG, "joinHost: connected successfully to $ip:$port")
    }

    suspend fun leaveHost() {
        client?.disconnect()
        client = null
        syncConsumerJob?.cancel()
        syncConsumerJob?.join()
        syncConsumerJob = null
        // DB 恢复本地（IO 线程）
        withContext(Dispatchers.IO) {
            databaseHelper.restoreLocalMode()
        }
        databaseHelper.emitDataChange()
        _dataVersion.value++
        _status.value = SyncStatus(SyncRole.NONE)
        Log.d(TAG, "Left host, restored local DB")
    }

    // === 工具方法 ===

    /** 获取本机局域网 IPv4 地址 */
    fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.filter { !it.isLoopbackAddress && it is Inet4Address }
                ?.map { it.hostAddress ?: "" }
                ?.firstOrNull { it.isNotEmpty() } ?: "127.0.0.1"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get local IP", e)
            "127.0.0.1"
        }
    }

    // === Repository 回调入口（主机端广播 / 客户端空操作） ===

    /** 菜品保存后广播完整实体（串行队列保证 FIFO） */
    fun onDishSaved(dishId: Long) {
        if (_status.value.role != SyncRole.HOST) return
        writeChannel.trySend {
            val dish = dishRepository.getDishByIdOnce(dishId) ?: return@trySend
            val ingredients = dishRepository.getDishIngredientsByDishIdOnce(dishId)
            val tags = dishRepository.getDishTagCrossRefsByDishIdOnce(dishId)
            val payload = dishToJson(dish).apply {
                put("ingredients", JSONArray().apply { ingredients.forEach { put(dishIngredientToJson(it)) } })
                put("tags", JSONArray().apply { tags.forEach { put(crossRefToJson(it)) } })
            }
            server?.append(SyncMessage(MsgType.DISH_SAVED, payload))
        }
    }

    /** 菜品删除后广播 */
    fun onDishDeleted(dishId: Long) {
        if (_status.value.role != SyncRole.HOST) return
        server?.append(SyncMessage(MsgType.DISH_DELETED, JSONObject().apply { put("id", dishId) }))
    }

    /** 订单保存后广播完整实体（串行队列保证 FIFO） */
    fun onOrderSaved(orderId: Long) {
        if (_status.value.role != SyncRole.HOST) return
        writeChannel.trySend {
            val order = orderRepository.getOrderByIdOnce(orderId) ?: return@trySend
            val dishes = orderRepository.getOrderDishesByOrderIdOnce(orderId)
            val payload = orderToJson(order).apply {
                put("dishes", JSONArray().apply { dishes.forEach { put(orderDishToJson(it)) } })
            }
            server?.append(SyncMessage(MsgType.ORDER_SAVED, payload))
        }
    }

    /** 订单删除后广播 */
    fun onOrderDeleted(orderId: Long) {
        if (_status.value.role != SyncRole.HOST) return
        server?.append(SyncMessage(MsgType.ORDER_DELETED, JSONObject().apply { put("id", orderId) }))
    }

    /** 食材保存后广播完整实体（串行队列保证 FIFO） */
    fun onIngredientSaved(ingredientId: Long) {
        if (_status.value.role != SyncRole.HOST) return
        writeChannel.trySend {
            val list = ingredientRepository.getAllIngredientsOnce()
            val ingredient = list.find { it.id == ingredientId } ?: return@trySend
            server?.append(SyncMessage(MsgType.INGREDIENT_SAVED, ingredientToJson(ingredient)))
        }
    }

    /** 食材删除后广播 */
    fun onIngredientDeleted(ingredientId: Long) {
        if (_status.value.role != SyncRole.HOST) return
        server?.append(SyncMessage(MsgType.INGREDIENT_DELETED, JSONObject().apply { put("id", ingredientId) }))
    }

    // === 全量快照（供 SyncServer /full 端点调用） ===

    suspend fun buildFullSnapshot(): JSONObject = withContext(Dispatchers.IO) {
        val dishes = dishRepository.getAllDishesOnce()
        val dishIngredients = mutableListOf<DishIngredient>()
        val dishTagCrossRefs = mutableListOf<DishTagCrossRef>()
        dishes.forEach { dish ->
            dishIngredients.addAll(dishRepository.getDishIngredientsByDishIdOnce(dish.id))
            dishTagCrossRefs.addAll(dishRepository.getDishTagCrossRefsByDishIdOnce(dish.id))
        }
        val orders = orderRepository.getAllOrdersOnce()
        val orderDishes = mutableListOf<OrderDish>()
        orders.forEach { order ->
            orderDishes.addAll(orderRepository.getOrderDishesByOrderIdOnce(order.id))
        }
        val ingredients = ingredientRepository.getAllIngredientsOnce()
        val categories = ingredientRepository.getAllIngredientCategoriesOnce()

        JSONObject().apply {
            put("dishes", JSONArray().apply { dishes.forEach { put(dishToJson(it)) } })
            put("dishIngredients", JSONArray().apply { dishIngredients.forEach { put(dishIngredientToJson(it)) } })
            put("dishTagCrossRefs", JSONArray().apply { dishTagCrossRefs.forEach { put(crossRefToJson(it)) } })
            put("orders", JSONArray().apply { orders.forEach { put(orderToJson(it)) } })
            put("orderDishes", JSONArray().apply { orderDishes.forEach { put(orderDishToJson(it)) } })
            put("ingredients", JSONArray().apply { ingredients.forEach { put(ingredientToJson(it)) } })
            put("categories", JSONArray().apply { categories.forEach { put(categoryToJson(it)) } })
        }
    }

    // === POST /write 处理（主机端） ===

    private suspend fun handleWrite(body: JSONObject): JSONObject {
        val action = body.getString("action")
        val data = body.getJSONObject("data")
        val result: JSONObject = when (action) {
            "create_dish" -> {
                val dish = jsonToDish(data)
                val ingredients = data.optJSONArray("ingredients")?.let { arr ->
                    (0 until arr.length()).map { jsonToDishIngredient(arr.getJSONObject(it)) }
                } ?: emptyList()
                val tags = data.optJSONArray("tags")?.let { arr ->
                    (0 until arr.length()).map { jsonToCrossRef(arr.getJSONObject(it)) }
                } ?: emptyList()
                val id = dishRepository.saveDishWithIngredientsAndTags(dish, ingredients, tags)
                JSONObject().apply { put("id", id); put("status", "ok") }
            }
            "update_dish" -> {
                val dish = jsonToDish(data)
                val ingredients = data.optJSONArray("ingredients")?.let { arr ->
                    (0 until arr.length()).map { jsonToDishIngredient(arr.getJSONObject(it)) }
                } ?: emptyList()
                val tags = data.optJSONArray("tags")?.let { arr ->
                    (0 until arr.length()).map { jsonToCrossRef(arr.getJSONObject(it)) }
                } ?: emptyList()
                dishRepository.updateDishWithIngredientsAndTags(dish, ingredients, tags)
                JSONObject().apply { put("status", "ok") }
            }
            "delete_dish" -> {
                val dish = dishRepository.getDishByIdOnce(data.getLong("id"))
                if (dish == null) JSONObject().apply { put("error", "not found") }
                else {
                    dishRepository.deleteDishWithRelations(dish)
                    JSONObject().apply { put("status", "ok") }
                }
            }
            "create_order" -> {
                val order = jsonToOrder(data)
                val dishes = data.optJSONArray("dishes")?.let { arr ->
                    (0 until arr.length()).map { jsonToOrderDish(arr.getJSONObject(it)) }
                } ?: emptyList()
                val id = orderRepository.saveOrderWithDishes(order, dishes)
                JSONObject().apply { put("id", id); put("status", "ok") }
            }
            "update_order" -> {
                val order = jsonToOrder(data)
                val dishes = data.optJSONArray("dishes")?.let { arr ->
                    (0 until arr.length()).map { jsonToOrderDish(arr.getJSONObject(it)) }
                } ?: emptyList()
                orderRepository.updateOrderWithDishes(order, dishes)
                JSONObject().apply { put("status", "ok") }
            }
            "delete_order" -> {
                val order = orderRepository.getOrderByIdOnce(data.getLong("id"))
                if (order == null) JSONObject().apply { put("error", "not found") }
                else {
                    orderRepository.deleteOrderWithDishes(order)
                    JSONObject().apply { put("status", "ok") }
                }
            }
            "create_ingredient" -> {
                val ingredient = jsonToIngredient(data)
                val id = ingredientRepository.insertIngredient(ingredient)
                JSONObject().apply { put("id", id); put("status", "ok") }
            }
            "delete_ingredient" -> {
                val list = ingredientRepository.getAllIngredientsOnce()
                val ingredient = list.find { it.id == data.getLong("id") }
                if (ingredient == null) JSONObject().apply { put("error", "not found") }
                else {
                    ingredientRepository.deleteIngredient(ingredient)
                    JSONObject().apply { put("status", "ok") }
                }
            }
            else -> JSONObject().apply { put("error", "unknown action: $action") }
        }
        return result
    }

    // === 全量快照批量导入（客户端首次连接 /full） ===

    private suspend fun importFullSnapshot(json: JSONObject) = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            // 菜品（逐条导入，单条失败不影响其他）
            val dishesArr = json.optJSONArray("dishes") ?: JSONArray()
            var dishOk = 0; var dishFail = 0
            for (i in 0 until dishesArr.length()) {
                try {
                    val d = dishesArr.getJSONObject(i)
                    importDish(d)
                    dishOk++
                } catch (e: Exception) {
                    dishFail++
                    Log.e(TAG, "importFullSnapshot: dish[$i] failed — ${e.message}", e)
                }
            }
            // 订单
            val ordersArr = json.optJSONArray("orders") ?: JSONArray()
            var orderOk = 0; var orderFail = 0
            for (i in 0 until ordersArr.length()) {
                try {
                    val o = ordersArr.getJSONObject(i)
                    importOrder(o)
                    orderOk++
                } catch (e: Exception) {
                    orderFail++
                    Log.e(TAG, "importFullSnapshot: order[$i] failed — ${e.message}", e)
                }
            }
            // 食材
            val ingredientsArr = json.optJSONArray("ingredients") ?: JSONArray()
            var ingOk = 0; var ingFail = 0
            for (i in 0 until ingredientsArr.length()) {
                try {
                    val ing = ingredientsArr.getJSONObject(i)
                    importIngredient(ing)
                    ingOk++
                } catch (e: Exception) {
                    ingFail++
                    Log.e(TAG, "importFullSnapshot: ingredient[$i] failed — ${e.message}", e)
                }
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Full snapshot imported: dishes ${dishOk}ok/${dishFail}fail, orders ${orderOk}ok/${orderFail}fail, ingredients ${ingOk}ok/${ingFail}fail")
        } finally {
            db.endTransaction()
        }
    }

    // === 客户端同步事件处理 ===

    private suspend fun handleSyncEvent(msg: SyncMessage) {
        val payload = msg.payload ?: return
        try {
            when (msg.type) {
                MsgType.DISH_SAVED -> importDish(payload)
                MsgType.DISH_DELETED -> deleteDishById(payload.optLong("id"))
                MsgType.ORDER_SAVED -> importOrder(payload)
                MsgType.ORDER_DELETED -> deleteOrderById(payload.optLong("id"))
                MsgType.INGREDIENT_SAVED -> importIngredient(payload)
                MsgType.INGREDIENT_DELETED -> deleteIngredientById(payload.optLong("id"))
                else -> {}
            }
            _dataVersion.value++
        } catch (e: Exception) {
            Log.e(TAG, "handleSyncEvent: failed to process ${msg.type}, payload=${payload.keys().asSequence().toList()}", e)
            // 单条消息处理失败不应影响后续同步
        }
    }

    private suspend fun importDish(payload: JSONObject) {
        val dish = jsonToDish(payload)
        val ingredients = payload.optJSONArray("ingredients")?.let { arr ->
            (0 until arr.length()).map { jsonToDishIngredient(arr.getJSONObject(it)).copy(dishId = dish.id) }
        } ?: emptyList()
        val tags = payload.optJSONArray("tags")?.let { arr ->
            (0 until arr.length()).map { jsonToCrossRef(arr.getJSONObject(it)).copy(dishId = dish.id) }
        } ?: emptyList()
        // Upsert: 存在则更新，不存在则插入
        val existing = dishRepository.getDishByIdOnce(dish.id)
        if (existing != null) {
            dishRepository.updateDish(dish)
            dishRepository.deleteDishIngredientsByDishId(dish.id)
            dishRepository.deleteDishTagCrossRefsByDishId(dish.id)
        } else {
            dishRepository.insertDish(dish)
        }
        ingredients.forEach { dishRepository.insertDishIngredient(it) }
        tags.forEach { dishRepository.insertDishTagCrossRef(it) }
    }

    private suspend fun deleteDishById(dishId: Long) {
        val dish = dishRepository.getDishByIdOnce(dishId) ?: return
        dishRepository.deleteDishWithRelations(dish)
    }

    private suspend fun importOrder(payload: JSONObject) {
        val order = jsonToOrder(payload)
        val dishes = payload.optJSONArray("dishes")?.let { arr ->
            (0 until arr.length()).map { jsonToOrderDish(arr.getJSONObject(it)).copy(orderId = order.id) }
        } ?: emptyList()
        val existing = orderRepository.getOrderByIdOnce(order.id)
        if (existing != null) {
            orderRepository.updateOrder(order)
            orderRepository.deleteOrderDishesByOrderId(order.id)
        } else {
            orderRepository.insertOrder(order)
        }
        dishes.forEach { orderRepository.insertOrderDish(it) }
    }

    private suspend fun deleteOrderById(orderId: Long) {
        val order = orderRepository.getOrderByIdOnce(orderId) ?: return
        orderRepository.deleteOrderWithDishes(order)
    }

    private suspend fun importIngredient(payload: JSONObject) {
        val ingredient = jsonToIngredient(payload)
        val existing = ingredientRepository.getAllIngredientsOnce().find { it.id == ingredient.id }
        if (existing != null) {
            ingredientRepository.updateIngredient(ingredient)
        } else {
            ingredientRepository.insertIngredient(ingredient)
        }
    }

    private suspend fun deleteIngredientById(ingredientId: Long) {
        val list = ingredientRepository.getAllIngredientsOnce()
        val ingredient = list.find { it.id == ingredientId } ?: return
        ingredientRepository.deleteIngredient(ingredient)
    }

    // === JSON 序列化 ===

    private fun dishToJson(d: Dish) = JSONObject().apply {
        put("id", d.id)
        put("name", d.name)
        put("imageUrl", d.imageUrl ?: "")
        put("recipe", d.recipe ?: "")
        put("cookingTime", d.cookingTime ?: 0)
        put("difficulty", d.difficulty ?: "")
        put("createdAt", d.createdAt)
        put("updatedAt", d.updatedAt)
    }

    private fun dishIngredientToJson(di: DishIngredient) = JSONObject().apply {
        put("dishId", di.dishId)
        put("ingredientId", di.ingredientId)
        put("quantity", di.quantity ?: "")
    }

    private fun crossRefToJson(cr: DishTagCrossRef) = JSONObject().apply {
        put("dishId", cr.dishId)
        put("tagId", cr.tagId)
    }

    private fun orderToJson(o: Order) = JSONObject().apply {
        put("id", o.id)
        put("date", o.date)
        put("time", o.time)
        put("status", o.status)
        put("note", o.note ?: "")
        put("createdAt", o.createdAt)
    }

    private fun orderDishToJson(od: OrderDish) = JSONObject().apply {
        put("orderId", od.orderId)
        put("dishId", od.dishId)
        put("quantity", od.quantity)
    }

    private fun ingredientToJson(i: Ingredient) = JSONObject().apply {
        put("id", i.id)
        put("name", i.name)
        put("categoryId", i.categoryId)
        put("isDefault", i.isDefault)
        put("icon", i.icon ?: "")
    }

    private fun categoryToJson(c: IngredientCategory) = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("sortOrder", c.sortOrder)
    }

    // === JSON 反序列化 ===

    private fun jsonToDish(json: JSONObject) = Dish(
        id = json.optLong("id"),
        name = json.getString("name"),
        imageUrl = json.optString("imageUrl", "").ifEmpty { null },
        recipe = json.optString("recipe", "").ifEmpty { null },
        cookingTime = json.optInt("cookingTime", 0).takeIf { it > 0 },
        difficulty = json.optString("difficulty", "").ifEmpty { null },
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun jsonToDishIngredient(json: JSONObject) = DishIngredient(
        dishId = json.optLong("dishId"),
        ingredientId = json.optLong("ingredientId"),
        quantity = json.optString("quantity", "").ifEmpty { null }
    )

    private fun jsonToCrossRef(json: JSONObject) = DishTagCrossRef(
        dishId = json.optLong("dishId"),
        tagId = json.optLong("tagId")
    )

    private fun jsonToOrder(json: JSONObject) = Order(
        id = json.optLong("id"),
        date = json.getString("date"),
        time = json.getString("time"),
        status = json.optString("status", "pending"),
        note = json.optString("note", "").ifEmpty { null },
        createdAt = json.optLong("createdAt", System.currentTimeMillis())
    )

    private fun jsonToOrderDish(json: JSONObject) = OrderDish(
        orderId = json.optLong("orderId"),
        dishId = json.optLong("dishId"),
        quantity = json.optInt("quantity", 1)
    )

    private fun jsonToIngredient(json: JSONObject) = Ingredient(
        id = json.optLong("id"),
        name = json.getString("name"),
        categoryId = json.optLong("categoryId"),
        isDefault = json.optBoolean("isDefault"),
        icon = json.optString("icon", "").ifEmpty { null }
    )
}
