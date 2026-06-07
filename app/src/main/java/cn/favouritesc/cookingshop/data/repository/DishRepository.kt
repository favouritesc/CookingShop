package cn.favouritesc.cookingshop.data.repository

import cn.favouritesc.cookingshop.data.db.DatabaseHelper
import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.DishIngredient
import cn.favouritesc.cookingshop.data.db.DishTag
import cn.favouritesc.cookingshop.data.db.DishTagCrossRef
import cn.favouritesc.cookingshop.data.db.Ingredient
import kotlinx.coroutines.flow.Flow

class DishRepository(private val databaseHelper: DatabaseHelper) {
    // 同步广播回调（SyncManager 注入）
    var sync: ((Long) -> Unit)? = null
    var syncDelete: ((Long) -> Unit)? = null

    // ========== 菜品 CRUD ==========

    suspend fun insertDish(dish: Dish): Long {
        return databaseHelper.insertDish(dish)
    }

    suspend fun updateDish(dish: Dish) {
        databaseHelper.updateDish(dish)
    }

    suspend fun deleteDish(dish: Dish) {
        databaseHelper.deleteDish(dish)
    }

    fun getDishById(id: Long): Flow<Dish?> {
        return databaseHelper.getDishById(id)
    }

    suspend fun getDishByIdOnce(id: Long): Dish? {
        return databaseHelper.getDishByIdOnce(id)
    }

    fun getAllDishes(): Flow<List<Dish>> {
        return databaseHelper.getAllDishes()
    }

    suspend fun getAllDishesOnce(): List<Dish> {
        return databaseHelper.getAllDishesOnce()
    }

    fun searchDishes(query: String): Flow<List<Dish>> {
        return databaseHelper.searchDishes(query)
    }

    // ========== 菜品备菜关联 ==========

    suspend fun insertDishIngredient(dishIngredient: DishIngredient) {
        databaseHelper.insertDishIngredient(dishIngredient)
    }

    suspend fun deleteDishIngredientsByDishId(dishId: Long) {
        databaseHelper.deleteDishIngredientsByDishId(dishId)
    }

    fun getDishIngredientsByDishId(dishId: Long): Flow<List<DishIngredient>> {
        return databaseHelper.getDishIngredientsByDishId(dishId)
    }

    suspend fun getDishIngredientsByDishIdOnce(dishId: Long): List<DishIngredient> {
        return databaseHelper.getDishIngredientsByDishIdOnce(dishId)
    }

    // ========== 菜品标签关联 ==========

    suspend fun insertDishTagCrossRef(crossRef: DishTagCrossRef) {
        databaseHelper.insertDishTagCrossRef(crossRef)
    }

    suspend fun deleteDishTagCrossRefsByDishId(dishId: Long) {
        databaseHelper.deleteDishTagCrossRefsByDishId(dishId)
    }

    fun getDishTagsByDishId(dishId: Long): Flow<List<DishTag>> {
        return databaseHelper.getDishTagsByDishId(dishId)
    }

    suspend fun getDishTagCrossRefsByDishIdOnce(dishId: Long): List<DishTagCrossRef> {
        return databaseHelper.getDishTagCrossRefsByDishIdOnce(dishId)
    }

    // ========== 批量操作 ==========

    suspend fun saveDishWithIngredientsAndTags(
        dish: Dish,
        ingredients: List<DishIngredient>,
        tags: List<DishTagCrossRef>
    ): Long {
        val dishId = insertDish(dish)
        // 清除旧关联
        deleteDishIngredientsByDishId(dishId)
        deleteDishTagCrossRefsByDishId(dishId)
        // 插入新关联
        ingredients.forEach { ingredient ->
            insertDishIngredient(ingredient.copy(dishId = dishId))
        }
        tags.forEach { tag ->
            insertDishTagCrossRef(tag.copy(dishId = dishId))
        }
        sync?.invoke(dishId)
        return dishId
    }

    suspend fun updateDishWithIngredientsAndTags(
        dish: Dish,
        ingredients: List<DishIngredient>,
        tags: List<DishTagCrossRef>
    ) {
        updateDish(dish)
        // 清除旧关联
        deleteDishIngredientsByDishId(dish.id)
        deleteDishTagCrossRefsByDishId(dish.id)
        // 插入新关联
        ingredients.forEach { ingredient ->
            insertDishIngredient(ingredient.copy(dishId = dish.id))
        }
        tags.forEach { tag ->
            insertDishTagCrossRef(tag.copy(dishId = dish.id))
        }
        sync?.invoke(dish.id)
    }

    suspend fun deleteDishWithRelations(dish: Dish) {
        deleteDishIngredientsByDishId(dish.id)
        deleteDishTagCrossRefsByDishId(dish.id)
        deleteDish(dish)
        syncDelete?.invoke(dish.id)
    }
}
