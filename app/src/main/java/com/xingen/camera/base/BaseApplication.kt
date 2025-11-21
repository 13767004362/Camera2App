package com.xingen.camera.base

import android.app.Application

/**
 * Created by ${xingen} on 2017/10/20.
 */
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @JvmStatic
        var instance: BaseApplication? = null
            private set
    }
}
