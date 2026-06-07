package cn.favouritesc.cookingshop.ui.home

import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.data.repository.OrderRepository
import cn.favouritesc.cookingshop.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeViewModel(
    private val orderRepository: OrderRepository,
    private val dishRepository: DishRepository,
    private val ingredientRepository: IngredientRepository
) : BaseViewModel() {
    private val _todayOrders = MutableStateFlow<List<Order>>(emptyList())
    val todayOrders: StateFlow<List<Order>> = _todayOrders.asStateFlow()

    private val _todayOrderDishes = MutableStateFlow<Map<Long, List<OrderDish>>>(emptyMap())
    val todayOrderDishes: StateFlow<Map<Long, List<OrderDish>>> = _todayOrderDishes.asStateFlow()

    private val _todayDishDetails = MutableStateFlow<Map<Long, Dish>>(emptyMap())
    val todayDishDetails: StateFlow<Map<Long, Dish>> = _todayDishDetails.asStateFlow()

    private val _ingredientSummary = MutableStateFlow<Map<Ingredient, String>>(emptyMap())
    val ingredientSummary: StateFlow<Map<Ingredient, String>> = _ingredientSummary.asStateFlow()

    private val _todayDate = MutableStateFlow(LocalDate.now())
    val todayDate: StateFlow<LocalDate> = _todayDate.asStateFlow()

    init {
        loadTodayData()
    }

    fun loadTodayData() {
        val today = LocalDate.now()
        _todayDate.value = today
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        launchCoroutine {
            val orders = orderRepository.getOrdersByDateOnce(todayStr)
            _todayOrders.value = orders
            loadOrderDishes(orders)
            calculateIngredientSummary(orders)
            setLoading(false)
        }
    }

    private fun loadOrderDishes(orders: List<Order>) {
        launchCoroutine {
            val orderDishesMap = mutableMapOf<Long, List<OrderDish>>()
            val dishDetailsMap = mutableMapOf<Long, Dish>()

            orders.forEach { order ->
                val orderDishes = orderRepository.getOrderDishesByOrderIdOnce(order.id)
                orderDishesMap[order.id] = orderDishes

                orderDishes.forEach { orderDish ->
                    val dish = dishRepository.getDishByIdOnce(orderDish.dishId)
                    if (dish != null) {
                        dishDetailsMap[dish.id] = dish
                    }
                }
            }

            _todayOrderDishes.value = orderDishesMap
            _todayDishDetails.value = dishDetailsMap
        }
    }

    private fun calculateIngredientSummary(orders: List<Order>) {
        launchCoroutine {
            val summary = mutableMapOf<Ingredient, String>()
            val allIngredients = ingredientRepository.getAllIngredientsOnce()

            orders.forEach { order ->
                val orderDishes = orderRepository.getOrderDishesByOrderIdOnce(order.id)
                orderDishes.forEach { orderDish ->
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
            }

            _ingredientSummary.value = summary
        }
    }
}
