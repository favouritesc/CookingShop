package cn.favouritesc.cookingshop.ui.order

import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.DishIngredient
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class OrderCreateViewModel(
    private val orderRepository: OrderRepository,
    private val dishRepository: DishRepository,
    private val ingredientRepository: IngredientRepository
) : BaseViewModel(), DishSelectViewModel {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedTime = MutableStateFlow(LocalTime.now().withMinute(0).withSecond(0))
    val selectedTime: StateFlow<LocalTime> = _selectedTime.asStateFlow()

    private val _availableDishes = MutableStateFlow<List<Dish>>(emptyList())
    override val availableDishes: StateFlow<List<Dish>> = _availableDishes.asStateFlow()

    private val _selectedDishes = MutableStateFlow<List<OrderDish>>(emptyList())
    override val selectedDishes: StateFlow<List<OrderDish>> = _selectedDishes.asStateFlow()

    private val _selectedDishDetails = MutableStateFlow<List<Dish>>(emptyList())
    val selectedDishDetails: StateFlow<List<Dish>> = _selectedDishDetails.asStateFlow()

    private val _ingredientSummary = MutableStateFlow<Map<Ingredient, String>>(emptyMap())
    val ingredientSummary: StateFlow<Map<Ingredient, String>> = _ingredientSummary.asStateFlow()

    private val _createdSuccessfully = MutableStateFlow(false)
    val createdSuccessfully: StateFlow<Boolean> = _createdSuccessfully.asStateFlow()

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

    fun updateDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun updateTime(time: LocalTime) {
        _selectedTime.value = time
    }

    override fun addDish(dish: Dish) {
        val current = _selectedDishes.value.toMutableList()
        val existing = current.find { it.dishId == dish.id }
        if (existing != null) {
            current[current.indexOf(existing)] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(OrderDish(orderId = 0, dishId = dish.id, quantity = 1))
        }
        _selectedDishes.value = current
        updateSelectedDishDetails()
        updateIngredientSummary()
    }

    fun removeDish(dishId: Long) {
        _selectedDishes.value = _selectedDishes.value.filter { it.dishId != dishId }
        updateSelectedDishDetails()
        updateIngredientSummary()
    }

    fun updateDishQuantity(dishId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeDish(dishId)
            return
        }
        val current = _selectedDishes.value.toMutableList()
        val index = current.indexOfFirst { it.dishId == dishId }
        if (index != -1) {
            current[index] = current[index].copy(quantity = quantity)
            _selectedDishes.value = current
            updateIngredientSummary()
        }
    }

    private fun updateSelectedDishDetails() {
        launchCoroutine {
            val dishIds = _selectedDishes.value.map { it.dishId }
            dishRepository.getAllDishes().collect { allDishes ->
                _selectedDishDetails.value = allDishes.filter { it.id in dishIds }
                setLoading(false)
            }
        }
    }

    private fun updateIngredientSummary() {
        launchCoroutine {
            val summary = mutableMapOf<Ingredient, String>()
            _selectedDishes.value.forEach { orderDish ->
                dishRepository.getDishIngredientsByDishId(orderDish.dishId).collect { dishIngredients ->
                    dishIngredients.forEach { dishIngredient ->
                        ingredientRepository.getAllIngredients().collect { allIngredients ->
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
            }
            _ingredientSummary.value = summary
            setLoading(false)
        }
    }

    fun resetCreateState() {
        _createdSuccessfully.value = false
    }

    fun createOrder() {
        if (_selectedDishes.value.isEmpty()) {
            setError("请至少选择一个菜品")
            return
        }

        launchCoroutine {
            val order = Order(
                date = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE),
                time = _selectedTime.value.format(DateTimeFormatter.ofPattern("HH:mm"))
            )
            orderRepository.saveOrderWithDishes(order, _selectedDishes.value)
            _createdSuccessfully.value = true
        }
    }
}
