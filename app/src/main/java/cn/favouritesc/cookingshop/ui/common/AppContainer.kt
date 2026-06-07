package cn.favouritesc.cookingshop.ui.common

import android.content.Context
import cn.favouritesc.cookingshop.data.db.DatabaseHelper
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.data.repository.OrderRepository
import cn.favouritesc.cookingshop.data.repository.TagRepository
import cn.favouritesc.cookingshop.sync.SyncManager

class AppContainer(private val context: Context) {
    private val databaseHelper = DatabaseHelper(context)

    val dishRepository = DishRepository(databaseHelper)
    val ingredientRepository = IngredientRepository(databaseHelper)
    val orderRepository = OrderRepository(databaseHelper)
    val tagRepository = TagRepository(databaseHelper)

    // 同步管理器（在 app 层初始化，可选）
    var syncManager: SyncManager? = null
        private set

    fun initSync() {
        syncManager = SyncManager(dishRepository, orderRepository, ingredientRepository, databaseHelper, context)
        // 注入广播回调到各 repository（主机端自动广播完整实体，客户端空操作）
        dishRepository.sync = { id -> syncManager?.onDishSaved(id) }
        dishRepository.syncDelete = { id -> syncManager?.onDishDeleted(id) }
        orderRepository.sync = { id -> syncManager?.onOrderSaved(id) }
        orderRepository.syncDelete = { id -> syncManager?.onOrderDeleted(id) }
        ingredientRepository.sync = { id -> syncManager?.onIngredientSaved(id) }
        ingredientRepository.syncDelete = { id -> syncManager?.onIngredientDeleted(id) }
    }
}
