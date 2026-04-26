package com.gem.neteasecloudmd.api

object ApiProvider {
    @Volatile
    private var instance: NeteaseApiService? = null

    fun get(): NeteaseApiService {
        return instance ?: synchronized(this) {
            instance ?: NeteaseApiService().also { instance = it }
        }
    }
}
