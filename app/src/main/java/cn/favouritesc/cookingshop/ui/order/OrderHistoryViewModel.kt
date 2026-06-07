package cn.favouritesc.cookingshop.ui.order

import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.OrderRepository
import cn.favouritesc.cookingshop.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class DateRange {
    ONE_DAY,
    SEVEN_DAYS,
    THIRTY_DAYS,
    CUSTOM
}

class OrderHistoryViewModel(
    private val orderRepository: OrderRepository,
    private val dishRepository: DishRepository
) : BaseViewModel() {
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _dateRange = MutableStateFlow(DateRange.ONE_DAY)
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    private val _customStartDate = MutableStateFlow<LocalDate?>(null)
    val customStartDate: StateFlow<LocalDate?> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow<LocalDate?>(null)
    val customEndDate: StateFlow<LocalDate?> = _customEndDate.asStateFlow()

    private val _orderDishes = MutableStateFlow<Map<Long, List<OrderDish>>>(emptyMap())
    val orderDishes: StateFlow<Map<Long, List<OrderDish>>> = _orderDishes.asStateFlow()

    private val _dishDetails = MutableStateFlow<Map<Long, Dish>>(emptyMap())
    val dishDetails: StateFlow<Map<Long, Dish>> = _dishDetails.asStateFlow()

    init {
        loadOrders()
    }

    fun updateDateRange(range: DateRange) {
        _dateRange.value = range
        loadOrders()
    }

    fun updateCustomStartDate(date: LocalDate?) {
        _customStartDate.value = date
        if (_dateRange.value == DateRange.CUSTOM) {
            loadOrders()
        }
    }

    fun updateCustomEndDate(date: LocalDate?) {
        _customEndDate.value = date
        if (_dateRange.value == DateRange.CUSTOM) {
            loadOrders()
        }
    }

    fun loadOrdersIncludingDate(date: LocalDate) {
        val today = LocalDate.now()
        _customStartDate.value = if (date.isBefore(today)) date else today
        _customEndDate.value = if (date.isAfter(today)) date else today
        _dateRange.value = DateRange.CUSTOM
        loadOrders()
    }

    fun loadOrders() {
        launchCoroutine {
            val today = LocalDate.now()
            val startDate = when (_dateRange.value) {
                DateRange.ONE_DAY -> today
                DateRange.SEVEN_DAYS -> today.minusDays(7)
                DateRange.THIRTY_DAYS -> today.minusDays(30)
                DateRange.CUSTOM -> _customStartDate.value ?: today
            }
            val endDate = when (_dateRange.value) {
                DateRange.CUSTOM -> _customEndDate.value ?: today
                else -> today
            }

            val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val orders = orderRepository.getOrdersByDateRangeOnce(startStr, endStr)
            _orders.value = orders
            loadOrderDishes(orders)
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
            }

            val allDishIds = orderDishesMap.values.flatten().map { it.dishId }.distinct()
            allDishIds.forEach { dishId ->
                val dish = dishRepository.getDishByIdOnce(dishId)
                dish?.let { dishDetailsMap[dishId] = it }
            }

            _orderDishes.value = orderDishesMap
            _dishDetails.value = dishDetailsMap
        }
    }
}
