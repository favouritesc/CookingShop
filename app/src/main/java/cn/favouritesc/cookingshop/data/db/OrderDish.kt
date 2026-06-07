package cn.favouritesc.cookingshop.data.db

data class OrderDish(
    val orderId: Long,
    val dishId: Long,
    val quantity: Int = 1
)
