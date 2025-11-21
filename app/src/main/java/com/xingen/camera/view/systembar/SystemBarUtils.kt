package com.xingen.camera.view.systembar

import android.view.View
import android.view.Window

/**
 * Created by ${xingen} on 2017/8/26.
 */
object SystemBarUtils {
    /**
     * 隐藏NavigatoinBar 和StatusBar
     * @param window
     */
    @JvmStatic
    fun setStickyStyle(window: Window) {
        val flag =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.setSystemUiVisibility(flag)
    }
}
