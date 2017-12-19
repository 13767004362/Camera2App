package com.xingen.camera.base;

import android.app.Application;

/**
 * Created by ${xingen} on 2017/10/20.
 */

public class BaseApplication extends Application {
    private static BaseApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance=this;
    }
    public static BaseApplication getInstance() {
        return instance;
    }
}
