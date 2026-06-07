package cn.favouritesc.cookingshop.data.db

enum class OrderStatus(val value: String, val displayName: String) {
    PENDING("pending", "未完成"),
    COMPLETED("completed", "已完成");

    companion object {
        fun fromValue(value: String): OrderStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}

data class Order(
    val id: Long = 0,
    val date: String,
    val time: String,
    val status: String = OrderStatus.PENDING.value,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val statusEnum: OrderStatus
        get() = OrderStatus.fromValue(status)
}
