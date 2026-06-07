package cn.favouritesc.cookingshop.ui.order

import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.db.OrderStatus
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.data.repository.OrderRepository
import android.util.Log
import cn.favouritesc.cookingshop.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OrderEditViewModel(
    private val orderRepository: OrderRepository,
    private val dishRepository: DishRepository,
    private val ingredientRepository: IngredientRepository
) : BaseViewModel(), DishSelectViewModel {
    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order.asStateFlow()

    private val _orderDishes = MutableStateFlow<List<OrderDish>>(emptyList())
    val orderDishes: StateFlow<List<OrderDish>> = _orderDishes.asStateFlow()

    private val _availableDishes = MutableStateFlow<List<Dish>>(emptyList())
    override val availableDishes: StateFlow<List<Dish>> = _availableDishes.asStateFlow()

    override val selectedDishes: StateFlow<List<OrderDish>> = _orderDishes.asStateFlow()

    init {
        loadAvailableDishes()
    }

    private fun loadAvailableDishes() {
        launchCoroutine {
            dishRepository.getAllDishes().collect { dishes ->
                _availableDishes.value = dishes
                setLoading(false)
            }
        }
    }

    private val _dishDetails = MutableStateFlow<List<Dish>>(emptyList())
    val dishDetails: StateFlow<List<Dish>> = _dishDetails.asStateFlow()

    private val _ingredientSummary = MutableStateFlow<Map<Ingredient, String>>(emptyMap())
    val ingredientSummary: StateFlow<Map<Ingredient, String>> = _ingredientSummary.asStateFlow()

    private val _updatedSuccessfully = MutableStateFlow(false)
    val updatedSuccessfully: StateFlow<Boolean> = _updatedSuccessfully.asStateFlow()

    private val _deletedSuccessfully = MutableStateFlow(false)
    val deletedSuccessfully: StateFlow<Boolean> = _deletedSuccessfully.asStateFlow()

    private val _completedSuccessfully = MutableStateFlow(false)
    val completedSuccessfully: StateFlow<Boolean> = _completedSuccessfully.asStateFlow()

    fun resetEditState() {
        _deletedSuccessfully.value = false
        _updatedSuccessfully.value = false
        _completedSuccessfully.value = false
    }

    fun loadOrder(orderId: Long) {
        Log.d("CookingShop", "OrderEditVM.loadOrder called, orderId=$orderId, thread=${Thread.currentThread().name}")
        launchCoroutine {
            val order = orderRepository.getOrderByIdOnce(orderId)
            _order.value = order

            val dishes = orderRepository.getOrderDishesByOrderIdOnce(orderId)
            _orderDishes.value = dishes
            Log.d("CookingShop", "OrderEditVM.loadOrder done, _orderDishes.size=${dishes.size}, dishes=${dishes.map { it.dishId }}")

            updateDishDetails()
            updateIngredientSummary()
            setLoading(false)
        }
    }

    private fun updateDishDetails() {
        launchCollection {
            val dishIds = _orderDishes.value.map { it.dishId }
            val allDishes = dishRepository.getAllDishesOnce()
            _dishDetails.value = allDishes.filter { it.id in dishIds }
        }
    }

    private fun updateIngredientSummary() {
        launchCollection {
            val summary = mutableMapOf<Ingredient, String>()
            val allIngredients = ingredientRepository.getAllIngredientsOnce()

            _orderDishes.value.forEach { orderDish ->
                val dishIngredients = dishRepository.getDishIngredientsByDishIdOnce(orderDish.dishId)
                dishIngredients.forEach { dishIngredient ->
                    val ingredient = allIngredients.find { it.id == dishIngredient.ingredientId }
                    if (ingredient != null) {
                        val currentQuantity = summary[ingredient] ?: ""
                        val newQuantity = if (currentQuantity.isBlank()) {
                            "${dishIngredient.quantity} x${orderDish.quantity}"
                        } else {
                            "$currentQuantity + ${dishIngredient.quantity} x${orderDish.quantity}"
                        }
                        summary[ingredient] = newQuantity
                    }
                }
            }
            _ingredientSummary.value = summary
        }
    }

    override fun addDish(dish: Dish) {
        val before = _orderDishes.value.map { it.dishId to it.quantity }
        Log.d("CookingShop", "OrderEditVM.addDish called, dish=${dish.name}(id=${dish.id}), _order=${_order.value?.id}, before=$before")
        val current = _orderDishes.value.toMutableList()
        val existing = current.find { it.dishId == dish.id }
        if (existing != null) {
            current[current.indexOf(existing)] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(OrderDish(orderId = _order.value?.id ?: 0, dishId = dish.id, quantity = 1))
        }
        _orderDishes.value = current
        Log.d("CookingShop", "OrderEditVM.addDish done, after=${_orderDishes.value.map { it.dishId to it.quantity }}")

        // 增量持久化：只插入新增的关联
        val orderId = _order.value?.id ?: 0L
        if (orderId != 0L && existing == null) {
            launchCollection {
                orderRepository.insertOrderDish(OrderDish(orderId = orderId, dishId = dish.id, quantity = 1))
                Log.d("CookingShop", "OrderEditVM.addDish: inserted to DB dishId=${dish.id}")
            }
        }

        updateDishDetails()
        updateIngredientSummary()
    }

    fun removeDish(dishId: Long) {
        _orderDishes.value = _orderDishes.value.filter { it.dishId != dishId }

        // 增量持久化：只删除指定的关联
        val orderId = _order.value?.id ?: 0L
        if (orderId != 0L) {
            launchCollection {
                orderRepository.deleteOrderDish(orderId, dishId)
                Log.d("CookingShop", "OrderEditVM.removeDish: deleted from DB dishId=$dishId")
            }
        }

        updateDishDetails()
        updateIngredientSummary()
    }

    fun updateDishQuantity(dishId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeDish(dishId)
            return
        }
        val current = _orderDishes.value.toMutableList()
        val index = current.indexOfFirst { it.dishId == dishId }
        if (index != -1) {
            current[index] = current[index].copy(quantity = quantity)
            _orderDishes.value = current

            // 增量持久化：只更新数量
            val orderId = _order.value?.id ?: 0L
            if (orderId != 0L) {
                launchCollection {
                    orderRepository.updateOrderDishQuantity(orderId, dishId, quantity)
                    Log.d("CookingShop", "OrderEditVM.updateDishQuantity: updated DB dishId=$dishId qty=$quantity")
                }
            }

            updateIngredientSummary()
        }
    }

    fun updateOrder() {
        val order = _order.value ?: return
        launchCoroutine {
            orderRepository.updateOrderWithDishes(order, _orderDishes.value)
            _updatedSuccessfully.value = true
        }
    }

    fun deleteOrder() {
        val order = _order.value ?: return
        launchCoroutine {
            orderRepository.deleteOrderWithDishes(order)
            _deletedSuccessfully.value = true
        }
    }

    fun completeOrder() {
        val order = _order.value ?: return
        launchCoroutine {
            val completedOrder = order.copy(status = OrderStatus.COMPLETED.value)
            orderRepository.updateOrder(completedOrder)
            _completedSuccessfully.value = true
        }
    }
}
