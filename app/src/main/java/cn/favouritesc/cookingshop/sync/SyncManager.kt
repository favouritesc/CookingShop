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
    private val filesDir = context.filesDir

    private var server: SyncServer? = null
    private var client: SyncClient? = null
    private var syncConsumerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 串行广播队列：保证主机端消息按 FIFO 顺序写入 changelog
    private val writeChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    // 处理来自远端的同步事件时，抑制本地 sync 回调，避免循环同步
    @Volatile private var suppressSyncCallback = false

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

    private val _toastMessage = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)
    /** 同步操作的 Toast 消息（UI 层收集后显示 2s 消失） */
    val toastMessage: SharedFlow<String> = _toastMessage

    // === 主机模式 ===

    fun startHost(port: Int = 8765) {
        _hostPort.value = port
        _hostIp.value = getLocalIpAddress()
        server = SyncServer(
            port,
            snapshotProvider = { buildFullSnapshot() },
            writeHandler = { json -> handleWrite(json) },
            filesDir = filesDir
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

    // === Repository 回调入口（主机端广播 / 客户端转发） ===

    /** 菜品保存后广播完整实体（串行队列保证 FIFO） */
    fun onDishSaved(dishId: Long) {
        if (suppressSyncCallback) return
        when (_status.value.role) {
            SyncRole.HOST -> {
                writeChannel.trySend {
                    val dish = dishRepository.getDishByIdOnce(dishId) ?: return@trySend
                    val ingredients = dishRepository.getDishIngredientsByDishIdOnce(dishId)
                    val tags = dishRepository.getDishTagCrossRefsByDishIdOnce(dishId)
                    val imageBaseUrl = "http://${_hostIp.value}:${_hostPort.value}/image/"
                    val payload = dishToJson(dish).apply {
                        val img = optString("imageUrl", "")
                        if (img.startsWith("/")) {
                            put("imageUrl", imageBaseUrl + java.net.URLEncoder.encode(img, "UTF-8"))
                        }
                        put("ingredients", JSONArray().apply { ingredients.forEach { put(dishIngredientToJson(it)) } })
                        put("tags", JSONArray().apply { tags.forEach { put(crossRefToJson(it)) } })
                    }
                    server?.append(SyncMessage(MsgType.DISH_SAVED, payload))
                    _toastMessage.tryEmit("已同步菜品: ${dish.name}")
                }
            }
            SyncRole.CLIENT -> scope.launch {
                try {
                    val dish = dishRepository.getDishByIdOnce(dishId) ?: return@launch
                    val ingredients = dishRepository.getDishIngredientsByDishIdOnce(dishId)
                    val tags = dishRepository.getDishTagCrossRefsByDishIdOnce(dishId)
                    val payload = dishToJson(dish).apply {
                        put("ingredients", JSONArray().apply { ingredients.forEach { put(dishIngredientToJson(it)) } })
                        put("tags", JSONArray().apply { tags.forEach { put(crossRefToJson(it)) } })
                    }
                    val body = JSONObject().apply {
                        put("action", "create_dish")
                        put("data", payload)
                    }
                    client?.postWrite(_hostIp.value, _hostPort.value, body)
                    _toastMessage.tryEmit("已同步菜品到主机: ${dish.name}")
                    Log.d(TAG, "Client forwarded dish save: $dishId")
                } catch (e: Exception) {
                    Log.e(TAG, "Client forward dish save failed", e)
                    _toastMessage.tryEmit("菜品同步失败")
                }
            }
            else -> {}
        }
    }

    /** 菜品删除后广播 */
    fun onDishDeleted(dishId: Long) {
        if (suppressSyncCallback) return
        when (_status.value.role) {
            SyncRole.HOST -> {
                server?.append(SyncMessage(MsgType.DISH_DELETED, JSONObject().apply { put("id", dishId) }))
                _toastMessage.tryEmit("已同步删除菜品")
            }
            SyncRole.CLIENT -> scope.launch {
                try {
                    val body = JSONObject().apply {
                        put("action", "delete_dish")
                        put("data", JSONObject().apply { put("id", dishId) })
                    }
                    client?.postWrite(_hostIp.value, _hostPort.value, body)
                    _toastMessage.tryEmit("已同步删除菜品到主机")
                    Log.d(TAG, "Client forwarded dish delete: $dishId")
                } catch (e: Exception) {
                    Log.e(TAG, "Client forward dish delete failed", e)
                    _toastMessage.tryEmit("菜品删除同步失败")
                }
            }
            else -> {}
        }
    }

    /** 订单保存后广播完整实体（串行队列保证 FIFO） */
    fun onOrderSaved(orderId: Long) {
        if (suppressSyncCallback) return
        when (_status.value.role) {
            SyncRole.HOST -> {
                writeChannel.trySend {
                    val order = orderRepository.getOrderByIdOnce(orderId) ?: return@trySend
                    val dishes = orderRepository.getOrderDishesByOrderIdOnce(orderId)
                    val payload = orderToJson(order).apply {
                        put("dishes", JSONArray().apply { dishes.forEach { put(orderDishToJson(it)) } })
                    }
                    server?.append(SyncMessage(MsgType.ORDER_SAVED, payload))
                    _toastMessage.tryEmit("已同步订单 #${orderId}")
                }
            }
            SyncRole.CLIENT -> scope.launch {
                try {
                    val order = orderRepository.getOrderByIdOnce(orderId) ?: return@launch
                    val dishes = orderRepository.getOrderDishesByOrderIdOnce(orderId)
                    val payload = orderToJson(order).apply {
                        put("dishes", JSONArray().apply { dishes.forEach { put(orderDishToJson(it)) } })
                    }
                    val body = JSONObject().apply {
                        put("action", "create_order")
                        put("data", payload)
                    }
                    client?.postWrite(_hostIp.value, _hostPort.value, body)
                    _toastMessage.tryEmit("已同步订单到主机 #${orderId}")
                    Log.d(TAG, "Client forwarded order save: $orderId")
                } catch (e: Exception) {
                    Log.e(TAG, "Client forward order save failed", e)
                    _toastMessage.tryEmit("订单同步失败")
                }
            }
            else -> {}
        }
    }

    /** 订单删除后广播 */
    fun onOrderDeleted(orderId: Long) {
        if (suppressSyncCallback) return
        when (_status.value.role) {
            SyncRole.HOST -> {
                server?.append(SyncMessage(MsgType.ORDER_DELETED, JSONObject().apply { put("id", orderId) }))
                _toastMessage.tryEmit("已同步删除订单 #${orderId}")
            }
            SyncRole.CLIENT -> scope.launch {
                try {
                    val body = JSONObject().apply {
                        put("action", "delete_order")
                        put("data", JSONObject().apply { put("id", orderId) })
                    }
                    client?.postWrite(_hostIp.value, _hostPort.value, body)
                    _toastMessage.tryEmit("已同步删除订单到主机 #${orderId}")
                    Log.d(TAG, "Client forwarded order delete: $orderId")
                } catch (e: Exception) {
                    Log.e(TAG, "Client forward order delete failed", e)
                    _toastMessage.tryEmit("订单删除同步失败")
                }
            }
            else -> {}
        }
    }

    /** 食材保存后广播完整实体（串行队列保证 FIFO） */
    fun onIngredientSaved(ingredientId: Long) {
        if (suppressSyncCallback) return
        when (_status.value.role) {
            SyncRole.HOST -> {
                writeChannel.trySend {
                    val list = ingredientRepository.getAllIngredientsOnce()
                    val ingredient = list.find { it.id == ingredientId } ?: return@trySend
                    server?.append(SyncMessage(MsgType.INGREDIENT_SAVED, ingredientToJson(ingredient)))
                    _toastMessage.tryEmit("已同步食材: ${ingredient.name}")
                }
            }
            SyncRole.CLIENT -> scope.launch {
                try {
                    val list = ingredientRepository.getAllIngredientsOnce()
                    val ingredient = list.find { it.id == ingredientId } ?: return@launch
                    val body = JSONObject().apply {
                        put("action", "create_ingredient")
                        put("data", ingredientToJson(ingredient))
                    }
                    client?.postWrite(_hostIp.value, _hostPort.value, body)
                    _toastMessage.tryEmit("已同步食材到主机: ${ingredient.name}")
                    Log.d(TAG, "Client forwarded ingredient save: $ingredientId")
                } catch (e: Exception) {
                    Log.e(TAG, "Client forward ingredient save failed", e)
                    _toastMessage.tryEmit("食材同步失败")
                }
            }
            else -> {}
        }
    }

    /** 食材删除后广播 */
    fun onIngredientDeleted(ingredientId: Long) {
        if (suppressSyncCallback) return
        when (_status.value.role) {
            SyncRole.HOST -> {
                server?.append(SyncMessage(MsgType.INGREDIENT_DELETED, JSONObject().apply { put("id", ingredientId) }))
                _toastMessage.tryEmit("已同步删除食材")
            }
            SyncRole.CLIENT -> scope.launch {
                try {
                    val body = JSONObject().apply {
                        put("action", "delete_ingredient")
                        put("data", JSONObject().apply { put("id", ingredientId) })
                    }
                    client?.postWrite(_hostIp.value, _hostPort.value, body)
                    _toastMessage.tryEmit("已同步删除食材到主机")
                    Log.d(TAG, "Client forwarded ingredient delete: $ingredientId")
                } catch (e: Exception) {
                    Log.e(TAG, "Client forward ingredient delete failed", e)
                    _toastMessage.tryEmit("食材删除同步失败")
                }
            }
            else -> {}
        }
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
        val imageBaseUrl = "http://${_hostIp.value}:${_hostPort.value}/image/"

        JSONObject().apply {
            put("dishes", JSONArray().apply {
                dishes.forEach { d ->
                    put(dishToJson(d).apply {
                        // 将本地文件路径转换为 HTTP URL
                        val img = optString("imageUrl", "")
                        if (img.startsWith("/")) {
                            put("imageUrl", imageBaseUrl + java.net.URLEncoder.encode(img, "UTF-8"))
                        }
                    })
                }
            })
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
        Log.d(TAG, "handleWrite: action=$action, dataKeys=${data.keys().asSequence().toList()}")
        val result: JSONObject = when (action) {
            "create_dish" -> {
                val dish = jsonToDish(data)
                val ingredients = data.optJSONArray("ingredients")?.let { arr ->
                    (0 until arr.length()).map { jsonToDishIngredient(arr.getJSONObject(it)) }
                } ?: emptyList()
                val tags = data.optJSONArray("tags")?.let { arr ->
                    (0 until arr.length()).map { jsonToCrossRef(arr.getJSONObject(it)) }
                } ?: emptyList()
                // Upsert: 存在则更新，不存在则插入
                val existing = if (dish.id > 0) dishRepository.getDishByIdOnce(dish.id) else null
                if (existing != null) {
                    dishRepository.updateDishWithIngredientsAndTags(dish, ingredients, tags)
                    JSONObject().apply { put("id", dish.id); put("status", "updated") }
                } else {
                    val id = dishRepository.saveDishWithIngredientsAndTags(dish, ingredients, tags)
                    JSONObject().apply { put("id", id); put("status", "created") }
                }
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
                // Upsert
                val existing = if (order.id > 0) orderRepository.getOrderByIdOnce(order.id) else null
                if (existing != null) {
                    orderRepository.updateOrderWithDishes(order, dishes)
                    JSONObject().apply { put("id", order.id); put("status", "updated") }
                } else {
                    val id = orderRepository.saveOrderWithDishes(order, dishes)
                    JSONObject().apply { put("id", id); put("status", "created") }
                }
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
                // Upsert
                val existing = if (ingredient.id > 0) ingredientRepository.getAllIngredientsOnce().find { it.id == ingredient.id } else null
                if (existing != null) {
                    ingredientRepository.updateIngredient(ingredient)
                    JSONObject().apply { put("id", ingredient.id); put("status", "updated") }
                } else {
                    val id = ingredientRepository.insertIngredient(ingredient)
                    JSONObject().apply { put("id", id); put("status", "created") }
                }
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
        // 主机端收到客户端写操作后，刷新自身 UI
        databaseHelper.emitDataChange()
        _dataVersion.value++
        Log.d(TAG, "handleWrite: result=${result.toString().take(100)}")
        return result
    }

    // === 全量快照批量导入（客户端首次连接 /full） ===

    private suspend fun importFullSnapshot(json: JSONObject) = withContext(Dispatchers.IO) {
        suppressSyncCallback = true
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
            // 订单-菜品关联
            val odArr = json.optJSONArray("orderDishes") ?: JSONArray()
            var odOk = 0; var odFail = 0
            for (i in 0 until odArr.length()) {
                try {
                    val od = jsonToOrderDish(odArr.getJSONObject(i))
                    databaseHelper.insertOrderDish(od)
                    odOk++
                } catch (e: Exception) {
                    odFail++
                    Log.e(TAG, "importFullSnapshot: orderDish[$i] failed — ${e.message}", e)
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
            // 菜品-食材关联
            val diArr = json.optJSONArray("dishIngredients") ?: JSONArray()
            var diOk = 0; var diFail = 0
            for (i in 0 until diArr.length()) {
                try {
                    val di = jsonToDishIngredient(diArr.getJSONObject(i))
                    databaseHelper.insertDishIngredient(di)
                    diOk++
                } catch (e: Exception) {
                    diFail++
                    Log.e(TAG, "importFullSnapshot: dishIngredient[$i] failed — ${e.message}", e)
                }
            }
            // 菜品-标签关联
            val crArr = json.optJSONArray("dishTagCrossRefs") ?: JSONArray()
            var crOk = 0; var crFail = 0
            for (i in 0 until crArr.length()) {
                try {
                    val cr = jsonToCrossRef(crArr.getJSONObject(i))
                    databaseHelper.insertDishTagCrossRef(cr)
                    crOk++
                } catch (e: Exception) {
                    crFail++
                    Log.e(TAG, "importFullSnapshot: dishTagCrossRef[$i] failed — ${e.message}", e)
                }
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Full snapshot imported: dishes ${dishOk}/${dishFail}fail, orders ${orderOk}/${orderFail}fail, orderDishes ${odOk}/${odFail}fail, ingredients ${ingOk}/${ingFail}fail, dishIngredients ${diOk}/${diFail}fail, tagCrossRefs ${crOk}/${crFail}fail")
        } finally {
            db.endTransaction()
            suppressSyncCallback = false
            // 确保所有 Flow 订阅者收到最新数据
            databaseHelper.emitDataChangeAfterModeSwitch()
        }
    }

    // === 客户端同步事件处理 ===

    private suspend fun handleSyncEvent(msg: SyncMessage) {
        val payload = msg.payload ?: return
        suppressSyncCallback = true
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
        } finally {
            suppressSyncCallback = false
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
