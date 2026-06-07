package cn.favouritesc.cookingshop.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.favouritesc.cookingshop.data.repository.DishRepository
import cn.favouritesc.cookingshop.data.repository.IngredientRepository
import cn.favouritesc.cookingshop.data.repository.OrderRepository
import cn.favouritesc.cookingshop.data.repository.TagRepository
import cn.favouritesc.cookingshop.ui.dish.DishDetailViewModel
import cn.favouritesc.cookingshop.ui.dish.DishEditViewModel
import cn.favouritesc.cookingshop.ui.dish.DishListViewModel
import cn.favouritesc.cookingshop.ui.home.HomeViewModel
import cn.favouritesc.cookingshop.ui.ingredient.IngredientSummaryViewModel
import cn.favouritesc.cookingshop.ui.ingredient.IngredientViewModel
import cn.favouritesc.cookingshop.ui.order.OrderCreateViewModel
import cn.favouritesc.cookingshop.ui.order.OrderEditViewModel
import cn.favouritesc.cookingshop.ui.order.OrderHistoryViewModel

class AppViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(appContainer.orderRepository, appContainer.dishRepository, appContainer.ingredientRepository) as T
            modelClass.isAssignableFrom(DishListViewModel::class.java) ->
                DishListViewModel(appContainer.dishRepository, appContainer.tagRepository) as T
            modelClass.isAssignableFrom(DishEditViewModel::class.java) ->
                DishEditViewModel(appContainer.dishRepository, appContainer.ingredientRepository, appContainer.tagRepository) as T
            modelClass.isAssignableFrom(DishDetailViewModel::class.java) ->
                DishDetailViewModel(appContainer.dishRepository, appContainer.ingredientRepository) as T
            modelClass.isAssignableFrom(OrderCreateViewModel::class.java) ->
                OrderCreateViewModel(appContainer.orderRepository, appContainer.dishRepository, appContainer.ingredientRepository) as T
            modelClass.isAssignableFrom(OrderEditViewModel::class.java) ->
                OrderEditViewModel(appContainer.orderRepository, appContainer.dishRepository, appContainer.ingredientRepository) as T
            modelClass.isAssignableFrom(OrderHistoryViewModel::class.java) ->
                OrderHistoryViewModel(appContainer.orderRepository, appContainer.dishRepository) as T
            modelClass.isAssignableFrom(IngredientViewModel::class.java) ->
                IngredientViewModel(appContainer.ingredientRepository) as T
            modelClass.isAssignableFrom(IngredientSummaryViewModel::class.java) ->
                IngredientSummaryViewModel(appContainer.orderRepository, appContainer.dishRepository, appContainer.ingredientRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
