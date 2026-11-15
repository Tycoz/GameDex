package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.network.NetworkClient
import java.util.concurrent.TimeUnit

class GameDexApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = NetworkClient.baseClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                // Wikimedia, IGDB, and open CDNs require a valid, non-generic User-Agent
                val requestWithHeaders = originalRequest.newBuilder()
                    .header(
                        "User-Agent",
                        "GameDexApp/1.0 (Android; +https://github.com/aistudio/gamedex; contact: info@gamedex.app)"
                    )
                    .build()
                chain.proceed(requestWithHeaders)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
