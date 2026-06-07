package cn.favouritesc.cookingshop.data.repository

import cn.favouritesc.cookingshop.data.db.DatabaseHelper
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import kotlinx.coroutines.flow.Flow

class OrderRepository(private val databaseHelper: DatabaseHelper) {
    var sync: ((Long) -> Unit)? = null
    var syncDelete: ((Long) -> Unit)? = null

    // ========== 订单 CRUD ==========

    suspend fun insertOrder(order: Order): Long {
        return databaseHelper.insertOrder(order)
    }

    suspend fun updateOrder(order: Order) {
        databaseHelper.updateOrder(order)
    }

    suspend fun deleteOrder(order: Order) {
        databaseHelper.deleteOrder(order)
    }

    fun getOrderById(id: Long): Flow<Order?> {
        return databaseHelper.getOrderById(id)
    }

    suspend fun getOrderByIdOnce(id: Long): Order? {
        return databaseHelper.getOrderByIdOnce(id)
    }

    fun getAllOrders(): Flow<List<Order>> {
        return databaseHelper.getAllOrders()
    }

    fun getOrdersByDate(date: String): Flow<List<Order>> {
        return databaseHelper.getOrdersByDate(date)
    }

    suspend fun getOrdersByDateOnce(date: String): List<Order> {
        return databaseHelper.getOrdersByDateOnce(date)
    }

    fun getOrdersByDateRange(startDate: String, endDate: String): Flow<List<Order>> {
        return databaseHelper.getOrdersByDateRange(startDate, endDate)
    }

    suspend fun getOrdersByDateRangeOnce(startDate: String, endDate: String): List<Order> {
        return databaseHelper.getOrdersByDateRangeOnce(startDate, endDate)
    }

    suspend fun getAllOrdersOnce(): List<Order> {
        return databaseHelper.getAllOrdersOnce()
    }

    // ========== 订单菜品关联 ==========

    suspend fun insertOrderDish(orderDish: OrderDish) {
        databaseHelper.insertOrderDish(orderDish)
    }

    suspend fun deleteOrderDishesByOrderId(orderId: Long) {
        databaseHelper.deleteOrderDishesByOrderId(orderId)
    }

    suspend fun deleteOrderDish(orderId: Long, dishId: Long) {
        databaseHelper.deleteOrderDish(orderId, dishId)
    }

    suspend fun updateOrderDishQuantity(orderId: Long, dishId: Long, quantity: Int) {
        databaseHelper.updateOrderDishQuantity(orderId, dishId, quantity)
    }

    fun getOrderDishesByOrderId(orderId: Long): Flow<List<OrderDish>> {
        return databaseHelper.getOrderDishesByOrderId(orderId)
    }

    suspend fun getOrderDishesByOrderIdOnce(orderId: Long): List<OrderDish> {
        return databaseHelper.getOrderDishesByOrderIdOnce(orderId)
    }

    // ========== 批量操作 ==========

    suspend fun saveOrderWithDishes(
        order: Order,
        dishes: List<OrderDish>
    ): Long {
        val orderId = insertOrder(order)
        dishes.forEach { dish ->
            insertOrderDish(dish.copy(orderId = orderId))
        }
        sync?.invoke(orderId)
        return orderId
    }

    suspend fun updateOrderWithDishes(
        order: Order,
        dishes: List<OrderDish>
    ) {
        updateOrder(order)
        deleteOrderDishesByOrderId(order.id)
        dishes.forEach { dish ->
            insertOrderDish(dish.copy(orderId = order.id))
        }
        sync?.invoke(order.id)
    }

    suspend fun deleteOrderWithDishes(order: Order) {
        deleteOrderDishesByOrderId(order.id)
        deleteOrder(order)
        syncDelete?.invoke(order.id)
    }
}
