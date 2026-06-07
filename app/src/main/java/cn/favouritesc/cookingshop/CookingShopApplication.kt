package cn.favouritesc.cookingshop

import android.app.Application
import cn.favouritesc.cookingshop.ui.common.AppContainer

class CookingShopApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
