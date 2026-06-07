package cn.favouritesc.cookingshop.ui.dish

import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.DishTag
import cn.favouritesc.cookingshop.data.db.TagType
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.TagRepository
import cn.favouritesc.cookingshop.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)

class DishListViewModel(
    private val dishRepository: DishRepository,
    private val tagRepository: TagRepository
) : BaseViewModel() {
    private val _dishes = MutableStateFlow<List<Dish>>(emptyList())
    val dishes: StateFlow<List<Dish>> = _dishes.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<DishTag>>(emptyList())
    val selectedTags: StateFlow<List<DishTag>> = _selectedTags.asStateFlow()

    private val _filteredDishes = MutableStateFlow<List<Dish>>(emptyList())
    val filteredDishes: StateFlow<List<Dish>> = _filteredDishes.asStateFlow()

    private val _availableTags = MutableStateFlow<Map<TagType, List<DishTag>>>(emptyMap())
    val availableTags: StateFlow<Map<TagType, List<DishTag>>> = _availableTags.asStateFlow()

    // 刷新触发器：每次值变化时重新查询数据库
    private val _refreshTrigger = MutableStateFlow(0)

    init {
        loadData()
    }

    private fun loadData() {
        launchCoroutine {
            // 使用 flatMapLatest：当 _refreshTrigger 变化时，取消旧的 flow 并创建新的
            _refreshTrigger.flatMapLatest {
                dishRepository.getAllDishes()
            }.collect { dishes ->
                _dishes.value = dishes
                applyFilters()
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

    /**
     * 刷新菜品列表 - 触发重新查询数据库
     */
    fun refreshDishes() {
        _refreshTrigger.value++
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun toggleTag(tag: DishTag) {
        val current = _selectedTags.value.toMutableList()
        if (current.any { it.id == tag.id }) {
            current.removeAll { it.id == tag.id }
        } else {
            current.add(tag)
        }
        _selectedTags.value = current
        applyFilters()
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedTags.value = emptyList()
        applyFilters()
    }

    private fun applyFilters() {
        launchCoroutine {
            val query = _searchQuery.value
            val selectedTagIds = _selectedTags.value.map { it.id }
            val allDishes = _dishes.value

            if (selectedTagIds.isEmpty()) {
                // No tag filter, just filter by query
                _filteredDishes.value = allDishes.filter { dish ->
                    query.isBlank() || dish.name.contains(query, ignoreCase = true)
                }
            } else {
                // Filter by both query and tags
                val filtered = mutableListOf<Dish>()
                allDishes.forEach { dish ->
                    val matchesQuery = query.isBlank() || dish.name.contains(query, ignoreCase = true)
                    if (matchesQuery) {
                        val dishTagCrossRefs = tagRepository.getDishTagCrossRefsByDishId(dish.id)
                        val dishTagIds = dishTagCrossRefs.map { it.tagId }
                        if (selectedTagIds.any { it in dishTagIds }) {
                            filtered.add(dish)
                        }
                    }
                }
                _filteredDishes.value = filtered
            }
        }
    }

    fun deleteDish(dish: Dish) {
        launchCoroutine {
            dishRepository.deleteDishWithRelations(dish)
            refreshDishes()  // 删除后刷新列表
        }
    }
}
