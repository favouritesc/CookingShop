package cn.favouritesc.cookingshop.ui.dish

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.DishIngredient
import cn.favouritesc.cookingshop.data.db.DishTag
import cn.favouritesc.cookingshop.data.db.DishTagCrossRef
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.TagType
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.data.repository.TagRepository
import cn.favouritesc.cookingshop.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class DishEditViewModel(
    private val dishRepository: DishRepository,
    private val ingredientRepository: IngredientRepository,
    private val tagRepository: TagRepository
) : BaseViewModel() {
    private val _dishId = MutableStateFlow<Long>(-1L)
    val dishId: StateFlow<Long> = _dishId.asStateFlow()

    private val _dishName = MutableStateFlow("")
    val dishName: StateFlow<String> = _dishName.asStateFlow()

    private val _recipe = MutableStateFlow("")
    val recipe: StateFlow<String> = _recipe.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _selectedIngredients = MutableStateFlow<List<DishIngredient>>(emptyList())
    val selectedIngredients: StateFlow<List<DishIngredient>> = _selectedIngredients.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<DishTag>>(emptyList())
    val selectedTags: StateFlow<List<DishTag>> = _selectedTags.asStateFlow()

    private val _availableIngredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val availableIngredients: StateFlow<List<Ingredient>> = _availableIngredients.asStateFlow()

    private val _availableTags = MutableStateFlow<Map<TagType, List<DishTag>>>(emptyMap())
    val availableTags: StateFlow<Map<TagType, List<DishTag>>> = _availableTags.asStateFlow()

    private val _savedSuccessfully = MutableStateFlow(false)
    val savedSuccessfully: StateFlow<Boolean> = _savedSuccessfully.asStateFlow()

    private val _cookingTime = MutableStateFlow(0)
    val cookingTime: StateFlow<Int> = _cookingTime.asStateFlow()

    private val _difficulty = MutableStateFlow("")
    val difficulty: StateFlow<String> = _difficulty.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        launchCoroutine {
            ingredientRepository.getAllIngredients().collect { ingredients ->
                _availableIngredients.value = ingredients
                setLoading(false)
            }
        }
        launchCoroutine {
            tagRepository.getAllDishTags().collect { tags ->
                _availableTags.value = tags.groupBy { it.type }
                setLoading(false)
            }
        }
    }

    fun loadDish(dishId: Long) {
        if (dishId == -1L) return
        _dishId.value = dishId
        launchCoroutine {
            dishRepository.getDishById(dishId).collect { dish ->
                dish?.let {
                    _dishName.value = it.name
                    _recipe.value = it.recipe ?: ""
                    _imageUri.value = it.imageUrl?.let { path -> Uri.fromFile(File(path)) }
                    _cookingTime.value = it.cookingTime ?: 0
                    _difficulty.value = it.difficulty ?: ""
                }
                setLoading(false)
            }
        }
        launchCoroutine {
            dishRepository.getDishIngredientsByDishId(dishId).collect {
                _selectedIngredients.value = it
                setLoading(false)
            }
        }
        launchCoroutine {
            dishRepository.getDishTagsByDishId(dishId).collect {
                _selectedTags.value = it
                setLoading(false)
            }
        }
    }

    fun updateDishName(name: String) {
        _dishName.value = name
    }

    fun updateRecipe(recipe: String) {
        _recipe.value = recipe
    }

    fun updateImageUri(uri: Uri?) {
        _imageUri.value = uri
    }

    fun resetSavedState() {
        _savedSuccessfully.value = false
    }

    fun resetForm() {
        _dishId.value = -1L
        _dishName.value = ""
        _recipe.value = ""
        _imageUri.value = null
        _selectedIngredients.value = emptyList()
        _selectedTags.value = emptyList()
        _savedSuccessfully.value = false
        _cookingTime.value = 0
        _difficulty.value = ""
    }

    fun updateCookingTime(minutes: Int) { _cookingTime.value = minutes }
    fun updateDifficulty(difficulty: String) { _difficulty.value = difficulty }

    fun addIngredient(ingredient: Ingredient, quantity: String) {
        val current = _selectedIngredients.value.toMutableList()
        val existing = current.find { it.ingredientId == ingredient.id }
        if (existing != null) {
            current[current.indexOf(existing)] = existing.copy(quantity = quantity)
        } else {
            current.add(DishIngredient(dishId = _dishId.value, ingredientId = ingredient.id, quantity = quantity))
        }
        _selectedIngredients.value = current
    }

    fun removeIngredient(ingredientId: Long) {
        _selectedIngredients.value = _selectedIngredients.value.filter { it.ingredientId != ingredientId }
    }

    fun toggleTag(tag: DishTag) {
        val current = _selectedTags.value.toMutableList()
        if (current.any { it.id == tag.id }) {
            current.removeAll { it.id == tag.id }
            // 移除烹饪时间标签时清零 cookingTime
            if (tag.type == TagType.COOKING_TIME) _cookingTime.value = 0
        } else {
            current.add(tag)
            // 选中烹饪时间标签时同步设置 cookingTime
            if (tag.type == TagType.COOKING_TIME) {
                tag.name.replace("分钟内", "").replace("分钟", "").toIntOrNull()?.let { _cookingTime.value = it }
            }
        }
        _selectedTags.value = current
    }

    fun saveDish(context: Context) {
        if (_dishName.value.isBlank()) {
            setError("请输入菜品名称")
            return
        }

        launchCoroutine {
            val imagePath = _imageUri.value?.let { uri ->
                saveImageToInternalStorage(context, uri)
            }

            val dish = Dish(
                id = _dishId.value,
                name = _dishName.value,
                recipe = _recipe.value.takeIf { it.isNotBlank() },
                imageUrl = imagePath,
                cookingTime = _cookingTime.value.takeIf { it > 0 },
                difficulty = _difficulty.value.takeIf { it.isNotBlank() },
                createdAt = if (_dishId.value == -1L) System.currentTimeMillis() else 0,
                updatedAt = System.currentTimeMillis()
            )

            val ingredients = _selectedIngredients.value
            val tags = _selectedTags.value.map { DishTagCrossRef(dishId = _dishId.value, tagId = it.id) }

            if (_dishId.value == -1L) {
                dishRepository.saveDishWithIngredientsAndTags(dish, ingredients, tags)
            } else {
                dishRepository.updateDishWithIngredientsAndTags(dish, ingredients, tags)
            }

            _savedSuccessfully.value = true
        }
    }

    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val compressedBitmap = compressBitmap(bitmap, 1024 * 1024) // 1MB
            val file = File(context.filesDir, "dish_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun compressBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var quality = 100
        var stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        while (stream.toByteArray().size > maxSize && quality > 10) {
            stream = java.io.ByteArrayOutputStream()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }
        return BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().size)
    }
}
