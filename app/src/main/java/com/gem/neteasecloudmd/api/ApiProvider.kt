package com.gem.neteasecloudmd.api

import android.content.Context

object ApiProvider {
    @Volatile
    private var instance: NeteaseApiService? = null

    @Suppress("UNUSED_PARAMETER")
    fun get(context: Context): NeteaseApiService {
        return instance ?: synchronized(this) {
            instance ?: NeteaseApiService().also { instance = it }
        }
    }
}
