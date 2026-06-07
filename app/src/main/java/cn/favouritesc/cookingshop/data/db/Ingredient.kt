package cn.favouritesc.cookingshop.data.db

data class Ingredient(
    val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val isDefault: Boolean = false,
    val icon: String? = null  // 用户自定义图标（emoji），null=自动匹配
)
