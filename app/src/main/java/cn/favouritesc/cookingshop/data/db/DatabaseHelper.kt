package cn.favouritesc.cookingshop.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    private val appCtx = context  // 存引用供 switchToHostMode/restoreLocalMode 使用

    init {
        // WAL + busy_timeout 优化并发写
        setWriteAheadLoggingEnabled(true)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.rawQuery("PRAGMA busy_timeout = 5000", null).use { it.moveToFirst() }
    }
    companion object {
        const val DATABASE_NAME = "cooking_shop_database"
        const val DATABASE_VERSION = 3
    }

    // 主机模式：非 null 时所有 DB 操作路由到 host DB，本地 DB 保持关闭
    @Volatile private var hostDb: SQLiteDatabase? = null

    private val dataChangeSignal = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)

    init {
        emitDataChange()
    }

    /** 供 SyncManager 在导入数据后手动触发 UI 刷新 */
    fun emitDataChange() {
        dataChangeSignal.tryEmit(Unit)
        Log.d("CookingDB", "emitDataChange: subscribers=${dataChangeSignal.subscriptionCount.value}")
    }

    /**
     * 切换到主机模式后，确保所有 Flow 订阅者收到最新数据。
     * 因为 switchToHostMode 会创建新 DB，旧的 replay 值可能已过期。
     */
    fun emitDataChangeAfterModeSwitch() {
        // 连续发射两次，确保即使上一个 replay 被消费也能收到新数据
        dataChangeSignal.tryEmit(Unit)
        dataChangeSignal.tryEmit(Unit)
        Log.d("CookingDB", "emitDataChangeAfterModeSwitch: emitted 2 signals")
    }

    override fun getWritableDatabase(): SQLiteDatabase = hostDb ?: super.getWritableDatabase()
    override fun getReadableDatabase(): SQLiteDatabase = hostDb ?: super.getReadableDatabase()

    // === DB 模式切换 ===

    /** 切换到主机模式：关闭本地 DB，创建/清空 host DB */
    @Synchronized
    fun switchToHostMode() {
        if (hostDb != null) {
            Log.d("CookingDB", "switchToHostMode: already in host mode, skipping")
            return
        }
        Log.d("CookingDB", "switchToHostMode: starting")
        // 强制 WAL checkpoint + 关闭 super 内部缓存的本地 DB 连接
        try {
            val localDb = super.getWritableDatabase()
            if (localDb.isOpen) {
                localDb.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            }
            Log.d("CookingDB", "switchToHostMode: WAL checkpoint done")
        } catch (e: Exception) {
            Log.w("CookingDB", "switchToHostMode: WAL checkpoint failed", e)
        }
        try { close(); Log.d("CookingDB", "switchToHostMode: local DB closed") }
        catch (e: Exception) { Log.w("CookingDB", "switchToHostMode: close failed", e) }
        // 创建新的 host DB 文件
        val hostPath = File(appCtx.getDatabasePath(DATABASE_NAME).parent, "cooking_shop_host.db")
        hostPath.parentFile?.mkdirs()
        // 用 deleteDatabase 清理 host 文件（连 -wal/-shm 一起清）
        try { appCtx.deleteDatabase("cooking_shop_host.db") } catch (e: Exception) {
            Log.w("CookingDB", "switchToHostMode: deleteDatabase failed", e)
        }
        Log.d("CookingDB", "switchToHostMode: creating host DB at $hostPath")
        val db = SQLiteDatabase.openOrCreateDatabase(hostPath, null)
        db.rawQuery("PRAGMA journal_mode=WAL", null).use { it.moveToFirst() }
        db.rawQuery("PRAGMA busy_timeout=5000", null).use { it.moveToFirst() }
        onCreate(db)  // 运行建表 DDL + 种子数据
        hostDb = db
        Log.d("CookingDB", "switchToHostMode: done, host DB ready")
    }

    /** 退出主机模式：关闭 host DB，删除文件，恢复本地 DB */
    @Synchronized
    fun restoreLocalMode() {
        val db = hostDb ?: return  // 已是本地模式
        hostDb = null
        try {
            if (db.isOpen) {
                db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            }
            db.close()
        } catch (_: Exception) {}
        try { appCtx.deleteDatabase("cooking_shop_host.db") } catch (_: Exception) {}
        Log.d("CookingDB", "Restored to local mode")
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建表
        db.execSQL("""
            CREATE TABLE dishes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                imageUrl TEXT,
                recipe TEXT,
                cookingTime INTEGER,
                difficulty TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE ingredient_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                sortOrder INTEGER DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE ingredients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                isDefault INTEGER DEFAULT 0,
                icon TEXT,
                FOREIGN KEY (categoryId) REFERENCES ingredient_categories(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE dish_ingredients (
                dishId INTEGER NOT NULL,
                ingredientId INTEGER NOT NULL,
                quantity TEXT,
                PRIMARY KEY (dishId, ingredientId),
                FOREIGN KEY (dishId) REFERENCES dishes(id) ON DELETE CASCADE,
                FOREIGN KEY (ingredientId) REFERENCES ingredients(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE dish_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE dish_tag_cross_ref (
                dishId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY (dishId, tagId),
                FOREIGN KEY (dishId) REFERENCES dishes(id) ON DELETE CASCADE,
                FOREIGN KEY (tagId) REFERENCES dish_tags(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'pending',
                note TEXT,
                createdAt INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE order_dishes (
                orderId INTEGER NOT NULL,
                dishId INTEGER NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY (orderId, dishId),
                FOREIGN KEY (orderId) REFERENCES orders(id) ON DELETE CASCADE,
                FOREIGN KEY (dishId) REFERENCES dishes(id) ON DELETE CASCADE
            )
        """)

        // 插入默认备菜分类和食材
        seedDefaultData(db)
    }

    private fun seedDefaultData(db: SQLiteDatabase) {
        // 默认分类（与 IconMap 中的分类对齐）
        val categories = listOf(
            "蔬菜" to 0,
            "肉类" to 1,
            "水产海鲜" to 2,
            "蛋奶豆制品" to 3,
            "主食面点" to 4,
            "水果" to 5,
            "调味料" to 6
        )
        val categoryIds = mutableMapOf<String, Long>()
        for ((name, sortOrder) in categories) {
            val values = ContentValues().apply {
                put("name", name)
                put("sortOrder", sortOrder)
            }
            val id = db.insert("ingredient_categories", null, values)
            categoryIds[name] = id
        }

        // 默认食材（isDefault=1, icon=null 由 IconMap 自动匹配 emoji）
        val defaultIngredients = mapOf(
            "蔬菜" to listOf(
                "白菜", "生菜", "菠菜", "油菜", "芹菜", "韭菜",
                "葱", "姜", "蒜", "洋葱", "辣椒", "青椒",
                "西红柿", "土豆", "胡萝卜", "白萝卜", "黄瓜", "茄子",
                "西兰花", "菜花", "豆角", "豆芽", "香菇", "金针菇",
                "藕", "玉米", "红薯", "香菜", "南瓜", "冬瓜"
            ),
            "肉类" to listOf(
                "猪肉", "牛肉", "羊肉", "鸡肉", "鸭肉",
                "排骨", "五花肉", "里脊", "培根", "火腿",
                "鸡翅", "鸡腿", "鸡胸", "牛腩", "肥牛", "羊排", "肉末",
                "腊肉", "香肠", "骨头"
            ),
            "水产海鲜" to listOf(
                "鱼肉", "虾", "虾仁", "蟹", "鱿鱼", "章鱼",
                "扇贝", "蛤蜊", "带鱼", "三文鱼", "鲈鱼", "鲫鱼",
                "草鱼", "小龙虾", "牡蛎"
            ),
            "蛋奶豆制品" to listOf(
                "鸡蛋", "鸭蛋", "鹌鹑蛋", "牛奶", "酸奶",
                "豆腐", "豆皮", "豆干", "腐竹", "千张",
                "奶酪", "黄油", "豆浆"
            ),
            "主食面点" to listOf(
                "米饭", "面条", "馒头", "饺子", "包子", "馄饨",
                "面包", "粉丝", "米粉", "意大利面", "粥",
                "煎饼", "年糕", "粽子", "汤圆", "油条"
            ),
            "水果" to listOf(
                "苹果", "香蕉", "橘子", "橙子", "葡萄", "草莓",
                "西瓜", "桃子", "梨", "柠檬", "芒果", "菠萝",
                "樱桃", "猕猴桃", "蓝莓", "柚子", "荔枝", "椰子"
            ),
            "调味料" to listOf(
                "盐", "糖", "酱油", "生抽", "老抽",
                "醋", "料酒", "蚝油", "豆瓣酱",
                "鸡精", "胡椒粉", "花椒", "八角",
                "香油", "食用油", "蜂蜜", "味精",
                "番茄酱", "芝麻", "桂皮", "香叶"
            )
        )
        for ((categoryName, ingredients) in defaultIngredients) {
            val categoryId = categoryIds[categoryName] ?: continue
            for (name in ingredients) {
                val values = ContentValues().apply {
                    put("name", name)
                    put("categoryId", categoryId)
                    put("isDefault", 1)
                }
                db.insert("ingredients", null, values)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 → v2: 添加 ingredients.icon 列
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE ingredients ADD COLUMN icon TEXT")
        }
        // v2 → v3: 为已有数据库补种默认备菜数据
        if (oldVersion < 3) {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM ingredient_categories", null)
            cursor.use {
                if (it.moveToFirst() && it.getInt(0) == 0) {
                    seedDefaultData(db)
                }
            }
        }
        // 仅当旧版本为 0 或完全重建时才删除表
        if (oldVersion == 0) {
            db.execSQL("DROP TABLE IF EXISTS order_dishes")
            db.execSQL("DROP TABLE IF EXISTS orders")
            db.execSQL("DROP TABLE IF EXISTS dish_tag_cross_ref")
            db.execSQL("DROP TABLE IF EXISTS dish_tags")
            db.execSQL("DROP TABLE IF EXISTS dish_ingredients")
            db.execSQL("DROP TABLE IF EXISTS ingredients")
            db.execSQL("DROP TABLE IF EXISTS ingredient_categories")
            db.execSQL("DROP TABLE IF EXISTS dishes")
            onCreate(db)
        }
    }

    // ========== 菜品 CRUD ==========

    suspend fun insertDish(dish: Dish): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (dish.id > 0) put("id", dish.id)
            put("name", dish.name)
            put("imageUrl", dish.imageUrl)
            put("recipe", dish.recipe)
            put("cookingTime", dish.cookingTime)
            put("difficulty", dish.difficulty)
            put("createdAt", dish.createdAt)
            put("updatedAt", dish.updatedAt)
        }
        val id = db.insert("dishes", null, values)
        emitDataChange()
        id
    }

    suspend fun updateDish(dish: Dish) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", dish.name)
            put("imageUrl", dish.imageUrl)
            put("recipe", dish.recipe)
            put("cookingTime", dish.cookingTime)
            put("difficulty", dish.difficulty)
            put("updatedAt", dish.updatedAt)
        }
        db.update("dishes", values, "id = ?", arrayOf(dish.id.toString()))
        emitDataChange()
    }

    suspend fun deleteDish(dish: Dish) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("dishes", "id = ?", arrayOf(dish.id.toString()))
        emitDataChange()
    }

    fun getAllDishes(): Flow<List<Dish>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val dishes = mutableListOf<Dish>()
            val db = readableDatabase
            val cursor = db.query("dishes", null, null, null, null, null, "createdAt DESC")
            cursor.use {
                while (it.moveToNext()) {
                    dishes.add(cursorToDish(it))
                }
            }
            dishes
        }
    }.flowOn(Dispatchers.IO)

    fun searchDishes(query: String): Flow<List<Dish>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val dishes = mutableListOf<Dish>()
            val db = readableDatabase
            val cursor = db.query("dishes", null, "name LIKE ?", arrayOf("%$query%"), null, null, "createdAt DESC")
            cursor.use {
                while (it.moveToNext()) {
                    dishes.add(cursorToDish(it))
                }
            }
            dishes
        }
    }.flowOn(Dispatchers.IO)

    fun getDishById(dishId: Long): Flow<Dish?> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val db = readableDatabase
            val cursor = db.query("dishes", null, "id = ?", arrayOf(dishId.toString()), null, null, null)
            cursor.use {
                if (it.moveToFirst()) {
                    cursorToDish(it)
                } else {
                    null
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun cursorToDish(cursor: Cursor): Dish {
        return Dish(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("imageUrl")),
            recipe = cursor.getString(cursor.getColumnIndexOrThrow("recipe")),
            cookingTime = cursor.getInt(cursor.getColumnIndexOrThrow("cookingTime")),
            difficulty = cursor.getString(cursor.getColumnIndexOrThrow("difficulty")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
        )
    }

    // ========== 备菜分类 CRUD ==========

    fun getAllIngredientCategories(): Flow<List<IngredientCategory>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val categories = mutableListOf<IngredientCategory>()
            val db = readableDatabase
            val cursor = db.query("ingredient_categories", null, null, null, null, null, "sortOrder")
            cursor.use {
                while (it.moveToNext()) {
                    categories.add(cursorToIngredientCategory(it))
                }
            }
            categories
        }
    }.flowOn(Dispatchers.IO)

    private fun cursorToIngredientCategory(cursor: Cursor): IngredientCategory {
        return IngredientCategory(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow("sortOrder"))
        )
    }

    // ========== 备菜 CRUD ==========

    suspend fun insertIngredient(ingredient: Ingredient): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (ingredient.id > 0) put("id", ingredient.id)
            put("name", ingredient.name)
            put("categoryId", ingredient.categoryId)
            put("isDefault", if (ingredient.isDefault) 1 else 0)
            put("icon", ingredient.icon)
        }
        val id = db.insert("ingredients", null, values)
        emitDataChange()
        id
    }

    suspend fun updateIngredient(ingredient: Ingredient) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", ingredient.name)
            put("categoryId", ingredient.categoryId)
            put("isDefault", if (ingredient.isDefault) 1 else 0)
            put("icon", ingredient.icon)
        }
        db.update("ingredients", values, "id = ?", arrayOf(ingredient.id.toString()))
        emitDataChange()
    }

    suspend fun deleteIngredient(ingredient: Ingredient) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("ingredients", "id = ?", arrayOf(ingredient.id.toString()))
        emitDataChange()
    }

    fun getAllIngredients(): Flow<List<Ingredient>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val ingredients = mutableListOf<Ingredient>()
            val db = readableDatabase
            val cursor = db.query("ingredients", null, null, null, null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    ingredients.add(cursorToIngredient(it))
                }
            }
            ingredients
        }
    }.flowOn(Dispatchers.IO)

    fun getIngredientsByCategory(categoryId: Long): Flow<List<Ingredient>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val ingredients = mutableListOf<Ingredient>()
            val db = readableDatabase
            val cursor = db.query("ingredients", null, "categoryId = ?", arrayOf(categoryId.toString()), null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    ingredients.add(cursorToIngredient(it))
                }
            }
            ingredients
        }
    }.flowOn(Dispatchers.IO)

    private fun cursorToIngredient(cursor: Cursor): Ingredient {
        val iconIndex = cursor.getColumnIndex("icon")
        return Ingredient(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            categoryId = cursor.getLong(cursor.getColumnIndexOrThrow("categoryId")),
            isDefault = cursor.getInt(cursor.getColumnIndexOrThrow("isDefault")) == 1,
            icon = if (iconIndex >= 0) cursor.getString(iconIndex) else null
        )
    }

    // ========== 菜品-备菜关联 CRUD ==========

    suspend fun insertDishIngredient(dishIngredient: DishIngredient) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("dishId", dishIngredient.dishId)
            put("ingredientId", dishIngredient.ingredientId)
            put("quantity", dishIngredient.quantity)
        }
        db.insert("dish_ingredients", null, values)
        emitDataChange()
    }

    suspend fun deleteDishIngredientsByDishId(dishId: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("dish_ingredients", "dishId = ?", arrayOf(dishId.toString()))
        emitDataChange()
    }

    fun getDishIngredientsByDishId(dishId: Long): Flow<List<DishIngredient>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val dishIngredients = mutableListOf<DishIngredient>()
            val db = readableDatabase
            val cursor = db.query("dish_ingredients", null, "dishId = ?", arrayOf(dishId.toString()), null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    dishIngredients.add(cursorToDishIngredient(it))
                }
            }
            dishIngredients
        }
    }.flowOn(Dispatchers.IO)

    private fun cursorToDishIngredient(cursor: Cursor): DishIngredient {
        return DishIngredient(
            dishId = cursor.getLong(cursor.getColumnIndexOrThrow("dishId")),
            ingredientId = cursor.getLong(cursor.getColumnIndexOrThrow("ingredientId")),
            quantity = cursor.getString(cursor.getColumnIndexOrThrow("quantity"))
        )
    }

    // ========== 菜品标签 CRUD ==========

    fun getAllDishTags(): Flow<List<DishTag>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val tags = mutableListOf<DishTag>()
            val db = readableDatabase
            val cursor = db.query("dish_tags", null, null, null, null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    tags.add(cursorToDishTag(it))
                }
            }
            tags
        }
    }.flowOn(Dispatchers.IO)

    fun getDishTagsByType(type: TagType): Flow<List<DishTag>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val tags = mutableListOf<DishTag>()
            val db = readableDatabase
            val cursor = db.query("dish_tags", null, "type = ?", arrayOf(type.name), null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    tags.add(cursorToDishTag(it))
                }
            }
            tags
        }
    }.flowOn(Dispatchers.IO)

    private fun cursorToDishTag(cursor: Cursor): DishTag {
        return DishTag(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            type = TagType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type")))
        )
    }

    // ========== 菜品-标签关联 CRUD ==========

    suspend fun insertDishTagCrossRef(crossRef: DishTagCrossRef) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("dishId", crossRef.dishId)
            put("tagId", crossRef.tagId)
        }
        db.insert("dish_tag_cross_ref", null, values)
        emitDataChange()
    }

    suspend fun deleteDishTagCrossRefsByDishId(dishId: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("dish_tag_cross_ref", "dishId = ?", arrayOf(dishId.toString()))
        emitDataChange()
    }

    fun getDishTagCrossRefsByDishId(dishId: Long): Flow<List<DishTagCrossRef>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val crossRefs = mutableListOf<DishTagCrossRef>()
            val db = readableDatabase
            val cursor = db.query("dish_tag_cross_ref", null, "dishId = ?", arrayOf(dishId.toString()), null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    crossRefs.add(cursorToDishTagCrossRef(it))
                }
            }
            crossRefs
        }
    }.flowOn(Dispatchers.IO)

    fun getDishTagsByDishId(dishId: Long): Flow<List<DishTag>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val tags = mutableListOf<DishTag>()
            val db = readableDatabase
            val cursor = db.rawQuery(
                """
                SELECT t.* FROM dish_tags t
                INNER JOIN dish_tag_cross_ref cr ON t.id = cr.tagId
                WHERE cr.dishId = ?
                """, arrayOf(dishId.toString())
            )
            cursor.use {
                while (it.moveToNext()) {
                    tags.add(cursorToDishTag(it))
                }
            }
            tags
        }
    }.flowOn(Dispatchers.IO)

    private fun cursorToDishTagCrossRef(cursor: Cursor): DishTagCrossRef {
        return DishTagCrossRef(
            dishId = cursor.getLong(cursor.getColumnIndexOrThrow("dishId")),
            tagId = cursor.getLong(cursor.getColumnIndexOrThrow("tagId"))
        )
    }

    suspend fun getDishTagCrossRefsByDishIdOnce(dishId: Long): List<DishTagCrossRef> = withContext(Dispatchers.IO) {
        val crossRefs = mutableListOf<DishTagCrossRef>()
        val db = readableDatabase
        val cursor = db.query("dish_tag_cross_ref", null, "dishId = ?", arrayOf(dishId.toString()), null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                crossRefs.add(cursorToDishTagCrossRef(it))
            }
        }
        crossRefs
    }

    // ========== 订单 CRUD ==========

    suspend fun insertOrder(order: Order): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (order.id > 0) put("id", order.id)
            put("date", order.date)
            put("time", order.time)
            put("status", order.status)
            put("note", order.note)
            put("createdAt", order.createdAt)
        }
        val id = db.insert("orders", null, values)
        emitDataChange()
        id
    }

    suspend fun updateOrder(order: Order) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("date", order.date)
            put("time", order.time)
            put("status", order.status)
            put("note", order.note)
        }
        db.update("orders", values, "id = ?", arrayOf(order.id.toString()))
        emitDataChange()
    }

    suspend fun deleteOrder(order: Order) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("orders", "id = ?", arrayOf(order.id.toString()))
        emitDataChange()
    }

    fun getAllOrders(): Flow<List<Order>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val orders = mutableListOf<Order>()
            val db = readableDatabase
            val cursor = db.query("orders", null, null, null, null, null, "date DESC, time DESC")
            cursor.use {
                while (it.moveToNext()) {
                    orders.add(cursorToOrder(it))
                }
            }
            orders
        }
    }.flowOn(Dispatchers.IO)

    fun getOrdersByDate(date: String): Flow<List<Order>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val orders = mutableListOf<Order>()
            val db = readableDatabase
            val cursor = db.query("orders", null, "date = ?", arrayOf(date), null, null, "time DESC")
            cursor.use {
                while (it.moveToNext()) {
                    orders.add(cursorToOrder(it))
                }
            }
            orders
        }
    }.flowOn(Dispatchers.IO)

    fun getOrdersByDateRange(startDate: String, endDate: String): Flow<List<Order>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val orders = mutableListOf<Order>()
            val db = readableDatabase
            val cursor = db.query("orders", null, "date BETWEEN ? AND ?", arrayOf(startDate, endDate), null, null, "date DESC, time DESC")
            cursor.use {
                while (it.moveToNext()) {
                    orders.add(cursorToOrder(it))
                }
            }
            orders
        }
    }.flowOn(Dispatchers.IO)

    fun getOrderById(orderId: Long): Flow<Order?> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val db = readableDatabase
            val cursor = db.query("orders", null, "id = ?", arrayOf(orderId.toString()), null, null, null)
            cursor.use {
                if (it.moveToFirst()) {
                    cursorToOrder(it)
                } else {
                    null
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun cursorToOrder(cursor: Cursor): Order {
        return Order(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
            time = cursor.getString(cursor.getColumnIndexOrThrow("time")),
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
            note = cursor.getString(cursor.getColumnIndexOrThrow("note")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
        )
    }

    // ========== 订单-菜品关联 CRUD ==========

    suspend fun insertOrderDish(orderDish: OrderDish) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("orderId", orderDish.orderId)
            put("dishId", orderDish.dishId)
            put("quantity", orderDish.quantity)
        }
        db.insert("order_dishes", null, values)
        emitDataChange()
    }

    suspend fun deleteOrderDishesByOrderId(orderId: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("order_dishes", "orderId = ?", arrayOf(orderId.toString()))
        emitDataChange()
    }

    suspend fun deleteOrderDish(orderId: Long, dishId: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("order_dishes", "orderId = ? AND dishId = ?", arrayOf(orderId.toString(), dishId.toString()))
        emitDataChange()
    }

    suspend fun updateOrderDishQuantity(orderId: Long, dishId: Long, quantity: Int) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("quantity", quantity)
        }
        db.update("order_dishes", values, "orderId = ? AND dishId = ?", arrayOf(orderId.toString(), dishId.toString()))
        emitDataChange()
    }

    fun getOrderDishesByOrderId(orderId: Long): Flow<List<OrderDish>> = dataChangeSignal.mapLatest {
        withContext(Dispatchers.IO) {
            val orderDishes = mutableListOf<OrderDish>()
            val db = readableDatabase
            val cursor = db.query("order_dishes", null, "orderId = ?", arrayOf(orderId.toString()), null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    orderDishes.add(cursorToOrderDish(it))
                }
            }
            orderDishes
        }
    }.flowOn(Dispatchers.IO)

    private fun cursorToOrderDish(cursor: Cursor): OrderDish {
        return OrderDish(
            orderId = cursor.getLong(cursor.getColumnIndexOrThrow("orderId")),
            dishId = cursor.getLong(cursor.getColumnIndexOrThrow("dishId")),
            quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))
        )
    }

    suspend fun getOrderDishesByOrderIdOnce(orderId: Long): List<OrderDish> = withContext(Dispatchers.IO) {
        val orderDishes = mutableListOf<OrderDish>()
        val db = readableDatabase
        val cursor = db.query("order_dishes", null, "orderId = ?", arrayOf(orderId.toString()), null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                orderDishes.add(cursorToOrderDish(it))
            }
        }
        orderDishes
    }

    suspend fun getOrdersByDateOnce(date: String): List<Order> = withContext(Dispatchers.IO) {
        val orders = mutableListOf<Order>()
        val db = readableDatabase
        val cursor = db.query("orders", null, "date = ?", arrayOf(date), null, null, "time DESC")
        cursor.use {
            while (it.moveToNext()) {
                orders.add(cursorToOrder(it))
            }
        }
        orders
    }

    suspend fun getDishIngredientsByDishIdOnce(dishId: Long): List<DishIngredient> = withContext(Dispatchers.IO) {
        val dishIngredients = mutableListOf<DishIngredient>()
        val db = readableDatabase
        val cursor = db.query("dish_ingredients", null, "dishId = ?", arrayOf(dishId.toString()), null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                dishIngredients.add(cursorToDishIngredient(it))
            }
        }
        dishIngredients
    }

    suspend fun getAllIngredientsOnce(): List<Ingredient> = withContext(Dispatchers.IO) {
        val ingredients = mutableListOf<Ingredient>()
        val db = readableDatabase
        val cursor = db.query("ingredients", null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                ingredients.add(cursorToIngredient(it))
            }
        }
        ingredients
    }

    suspend fun getOrdersByDateRangeOnce(startDate: String, endDate: String): List<Order> = withContext(Dispatchers.IO) {
        val orders = mutableListOf<Order>()
        val db = readableDatabase
        val cursor = db.query("orders", null, "date BETWEEN ? AND ?", arrayOf(startDate, endDate), null, null, "date DESC, time DESC")
        cursor.use {
            while (it.moveToNext()) {
                orders.add(cursorToOrder(it))
            }
        }
        orders
    }

    suspend fun getDishByIdOnce(dishId: Long): Dish? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query("dishes", null, "id = ?", arrayOf(dishId.toString()), null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                cursorToDish(it)
            } else {
                null
            }
        }
    }

    suspend fun getOrderByIdOnce(orderId: Long): Order? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query("orders", null, "id = ?", arrayOf(orderId.toString()), null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                cursorToOrder(it)
            } else {
                null
            }
        }
    }

    suspend fun getAllDishesOnce(): List<Dish> = withContext(Dispatchers.IO) {
        val dishes = mutableListOf<Dish>()
        val db = readableDatabase
        val cursor = db.query("dishes", null, null, null, null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                dishes.add(cursorToDish(it))
            }
        }
        dishes
    }

    suspend fun getAllOrdersOnce(): List<Order> = withContext(Dispatchers.IO) {
        val orders = mutableListOf<Order>()
        val db = readableDatabase
        val cursor = db.query("orders", null, null, null, null, null, "date DESC, time DESC")
        cursor.use {
            while (it.moveToNext()) {
                orders.add(cursorToOrder(it))
            }
        }
        orders
    }

    suspend fun getAllIngredientCategoriesOnce(): List<IngredientCategory> = withContext(Dispatchers.IO) {
        val categories = mutableListOf<IngredientCategory>()
        val db = readableDatabase
        val cursor = db.query("ingredient_categories", null, null, null, null, null, "sortOrder")
        cursor.use {
            while (it.moveToNext()) {
                categories.add(cursorToIngredientCategory(it))
            }
        }
        categories
    }
}
