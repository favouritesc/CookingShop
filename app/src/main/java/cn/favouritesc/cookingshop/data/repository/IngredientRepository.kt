package cn.favouritesc.cookingshop.data.repository

import cn.favouritesc.cookingshop.data.db.DatabaseHelper
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.IngredientCategory
import kotlinx.coroutines.flow.Flow

class IngredientRepository(private val databaseHelper: DatabaseHelper) {
    var sync: ((Long) -> Unit)? = null
    var syncDelete: ((Long) -> Unit)? = null

    // ========== 备菜分类 ==========

    fun getAllIngredientCategories(): Flow<List<IngredientCategory>> {
        return databaseHelper.getAllIngredientCategories()
    }

    suspend fun getAllIngredientCategoriesOnce(): List<IngredientCategory> {
        return databaseHelper.getAllIngredientCategoriesOnce()
    }

    // ========== 备菜 CRUD ==========

    suspend fun insertIngredient(ingredient: Ingredient): Long {
        val id = databaseHelper.insertIngredient(ingredient)
        sync?.invoke(id)
        return id
    }

    suspend fun updateIngredient(ingredient: Ingredient) {
        databaseHelper.updateIngredient(ingredient)
        sync?.invoke(ingredient.id)
    }

    suspend fun deleteIngredient(ingredient: Ingredient) {
        databaseHelper.deleteIngredient(ingredient)
        syncDelete?.invoke(ingredient.id)
    }

    fun getAllIngredients(): Flow<List<Ingredient>> {
        return databaseHelper.getAllIngredients()
    }

    suspend fun getAllIngredientsOnce(): List<Ingredient> {
        return databaseHelper.getAllIngredientsOnce()
    }

    fun getIngredientsByCategory(categoryId: Long): Flow<List<Ingredient>> {
        return databaseHelper.getIngredientsByCategory(categoryId)
    }
}
