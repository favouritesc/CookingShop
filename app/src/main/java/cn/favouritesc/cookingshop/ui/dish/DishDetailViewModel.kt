package cn.favouritesc.cookingshop.ui.dish

import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.DishIngredient
import cn.favouritesc.cookingshop.data.db.DishTag
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DishDetailViewModel(
    private val dishRepository: DishRepository,
    private val ingredientRepository: IngredientRepository
) : BaseViewModel() {
    private val _dish = MutableStateFlow<Dish?>(null)
    val dish: StateFlow<Dish?> = _dish.asStateFlow()

    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients.asStateFlow()

    private val _dishIngredients = MutableStateFlow<List<DishIngredient>>(emptyList())
    val dishIngredients: StateFlow<List<DishIngredient>> = _dishIngredients.asStateFlow()

    private val _tags = MutableStateFlow<List<DishTag>>(emptyList())
    val tags: StateFlow<List<DishTag>> = _tags.asStateFlow()

    fun loadDish(dishId: Long) {
        launchCoroutine {
            dishRepository.getDishById(dishId).collect { dish ->
                _dish.value = dish
                setLoading(false)
            }
        }
        launchCoroutine {
            dishRepository.getDishIngredientsByDishId(dishId).collect { dishIngredients ->
                _dishIngredients.value = dishIngredients
                // 加载对应的备菜详情
                val ingredientIds = dishIngredients.map { it.ingredientId }
                ingredientRepository.getAllIngredients().collect { allIngredients ->
                    _ingredients.value = allIngredients.filter { it.id in ingredientIds }
                }
                setLoading(false)
            }
        }
        launchCoroutine {
            dishRepository.getDishTagsByDishId(dishId).collect { tags ->
                _tags.value = tags
                setLoading(false)
            }
        }
    }
}
