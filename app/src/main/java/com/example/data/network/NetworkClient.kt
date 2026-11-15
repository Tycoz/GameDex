package com.example.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared base OkHttpClient so all network services and Coil reuse a single
 * ConnectionPool and Dispatcher thread pool, reducing memory and thread overhead.
 */
object NetworkClient {
    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
