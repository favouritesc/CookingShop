package cn.favouritesc.cookingshop.ui.ingredient

import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.IngredientCategory
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.data.repository.OrderRepository
import cn.favouritesc.cookingshop.ui.common.BaseViewModel
import cn.favouritesc.cookingshop.ui.order.DateRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class IngredientSummaryItem(
    val ingredient: Ingredient,
    val totalQuantity: String,
    val sourceDishes: List<String>
)

class IngredientSummaryViewModel(
    private val orderRepository: OrderRepository,
    private val dishRepository: DishRepository,
    private val ingredientRepository: IngredientRepository
) : BaseViewModel() {
    private val _dateRange = MutableStateFlow(DateRange.ONE_DAY)
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    private val _ingredientSummary = MutableStateFlow<Map<IngredientCategory, List<IngredientSummaryItem>>>(emptyMap())
    val ingredientSummary: StateFlow<Map<IngredientCategory, List<IngredientSummaryItem>>> = _ingredientSummary.asStateFlow()

    private val _categories = MutableStateFlow<List<IngredientCategory>>(emptyList())
    val categories: StateFlow<List<IngredientCategory>> = _categories.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        launchCoroutine {
            ingredientRepository.getAllIngredientCategories().collect { categories ->
                _categories.value = categories
                setLoading(false)
            }
        }
        loadSummary()
    }

    fun updateDateRange(range: DateRange) {
        _dateRange.value = range
        loadSummary()
    }

    fun loadSummary() {
        launchCoroutine {
            val today = LocalDate.now()
            val startDate = when (_dateRange.value) {
                DateRange.ONE_DAY -> today
                DateRange.SEVEN_DAYS -> today.minusDays(7)
                DateRange.THIRTY_DAYS -> today.minusDays(30)
                DateRange.CUSTOM -> today // Not used in summary
            }
            val endDate = today

            val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            orderRepository.getOrdersByDateRange(startStr, endStr).collect { orders ->
                calculateSummary(orders)
            }
        }
    }

    private suspend fun calculateSummary(orders: List<Order>) {
        val ingredientMap = mutableMapOf<Ingredient, IngredientSummaryItem>()
        val categories = _categories.value

        orders.forEach { order ->
            orderRepository.getOrderDishesByOrderId(order.id).collect { orderDishes ->
                orderDishes.forEach { orderDish ->
                    dishRepository.getDishById(orderDish.dishId).collect { dish ->
                        if (dish != null) {
                            dishRepository.getDishIngredientsByDishId(orderDish.dishId).collect { dishIngredients ->
                                dishIngredients.forEach { dishIngredient ->
                                    ingredientRepository.getAllIngredients().collect { allIngredients ->
                                        val ingredient = allIngredients.find { it.id == dishIngredient.ingredientId }
                                        if (ingredient != null) {
                                            val existing = ingredientMap[ingredient]
                                            val newQuantity = if (existing == null) {
                                                "${dishIngredient.quantity} x${orderDish.quantity}"
                                            } else {
                                                "${existing.totalQuantity} + ${dishIngredient.quantity} x${orderDish.quantity}"
                                            }
                                            val sourceDishes = existing?.sourceDishes?.toMutableList() ?: mutableListOf()
                                            if (!sourceDishes.contains(dish.name)) {
                                                sourceDishes.add(dish.name)
                                            }
                                            ingredientMap[ingredient] = IngredientSummaryItem(
                                                ingredient = ingredient,
                                                totalQuantity = newQuantity,
                                                sourceDishes = sourceDishes
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Group by category
        val grouped = categories.associateWith { category ->
            ingredientMap.values.filter { it.ingredient.categoryId == category.id }
        }.filter { it.value.isNotEmpty() }

        _ingredientSummary.value = grouped
    }
}
