package cn.favouritesc.cookingshop.data.repository

import cn.favouritesc.cookingshop.data.db.DatabaseHelper
import cn.favouritesc.cookingshop.data.db.DishTag
import cn.favouritesc.cookingshop.data.db.DishTagCrossRef
import cn.favouritesc.cookingshop.data.db.TagType
import kotlinx.coroutines.flow.Flow

class TagRepository(private val databaseHelper: DatabaseHelper) {
    // ========== 标签 CRUD ==========

    fun getAllDishTags(): Flow<List<DishTag>> {
        return databaseHelper.getAllDishTags()
    }

    fun getDishTagsByType(type: TagType): Flow<List<DishTag>> {
        return databaseHelper.getDishTagsByType(type)
    }

    suspend fun getDishTagCrossRefsByDishId(dishId: Long): List<DishTagCrossRef> {
        return databaseHelper.getDishTagCrossRefsByDishIdOnce(dishId)
    }
}
