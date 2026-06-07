package cn.favouritesc.cookingshop.data.db

data class DishIngredient(
    val dishId: Long,
    val ingredientId: Long,
    val quantity: String? = null
)
