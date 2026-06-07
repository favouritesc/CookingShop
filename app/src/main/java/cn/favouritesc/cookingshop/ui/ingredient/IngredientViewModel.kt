package cn.favouritesc.cookingshop.ui.ingredient

import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.IngredientCategory
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class IngredientViewModel(private val ingredientRepository: IngredientRepository) : BaseViewModel() {
    private val _categories = MutableStateFlow<List<IngredientCategory>>(emptyList())
    val categories: StateFlow<List<IngredientCategory>> = _categories.asStateFlow()

    private val _ingredientsByCategory = MutableStateFlow<Map<IngredientCategory, List<Ingredient>>>(emptyMap())
    val ingredientsByCategory: StateFlow<Map<IngredientCategory, List<Ingredient>>> = _ingredientsByCategory.asStateFlow()

    private val _editingIngredient = MutableStateFlow<Ingredient?>(null)
    val editingIngredient: StateFlow<Ingredient?> = _editingIngredient.asStateFlow()

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        launchCoroutine {
            combine(
                ingredientRepository.getAllIngredientCategories(),
                ingredientRepository.getAllIngredients()
            ) { categories, ingredients ->
                val grouped = categories.associateWith { category ->
                    ingredients.filter { it.categoryId == category.id }
                }
                Pair(categories, grouped)
            }.collect { (categories, grouped) ->
                _categories.value = categories
                _ingredientsByCategory.value = grouped
                setLoading(false)
            }
        }
    }

    fun updateIngredient(ingredient: Ingredient) {
        launchCoroutine {
            ingredientRepository.updateIngredient(ingredient)
        }
    }

    fun deleteIngredient(ingredient: Ingredient) {
        launchCoroutine {
            ingredientRepository.deleteIngredient(ingredient)
        }
    }

    fun showEditDialog(ingredient: Ingredient? = null) {
        _editingIngredient.value = ingredient
        _showEditDialog.value = true
    }

    fun hideEditDialog() {
        _editingIngredient.value = null
        _showEditDialog.value = false
    }

    fun saveIngredient(name: String, categoryId: Long, icon: String? = null) {
        val editing = _editingIngredient.value
        if (editing != null) {
            updateIngredient(editing.copy(name = name, categoryId = categoryId, icon = icon))
        } else {
            addIngredient(name, categoryId, icon)
        }
        hideEditDialog()
    }

    fun addIngredient(name: String, categoryId: Long, icon: String? = null) {
        launchCoroutine {
            ingredientRepository.insertIngredient(
                Ingredient(name = name, categoryId = categoryId, icon = icon)
            )
        }
    }
}
