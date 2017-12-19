package com.xingen.camera.view.systembar;

import android.view.View;
import android.view.Window;

/**
 * Created by ${xingen} on 2017/8/26.
 */

public class SystemBarUtils {

    /**
     * 隐藏NavigatoinBar 和StatusBar
     * @param window
     */
    public static void setStickyStyle(Window window){
        int flag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        window.getDecorView().setSystemUiVisibility(flag);
    }
}
