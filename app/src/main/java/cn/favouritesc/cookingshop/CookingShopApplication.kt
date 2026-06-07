package cn.favouritesc.cookingshop

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import cn.favouritesc.cookingshop.ui.common.AppContainer
import okhttp3.OkHttpClient

class CookingShopApplication : Application(), ImageLoaderFactory {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .build()
            }
            .build()
    }
}
