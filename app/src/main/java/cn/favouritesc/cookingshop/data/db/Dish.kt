package cn.favouritesc.cookingshop.data.db

data class Dish(
    val id: Long = 0,
    val name: String,
    val imageUrl: String? = null,
    val recipe: String? = null,
    val cookingTime: Int? = null,
    val difficulty: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
