package com.xingen.camera.utils.thread;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by ${xingen} on 2017/10/20.
 * <p>
 * 后台线程和对应Handler的管理类
 */

public class WorkThreadUtils {
    private final String thread_name = "Camera2WorkThread";
    /**
     * 后台线程处理

     */
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    public static WorkThreadUtils newInstance() {
         return new WorkThreadUtils();
    }

    /**
     * 开启一个线程和对应的Handler
     */
    public void startWorkThread() {
        startWorkThread(thread_name);
    }

    public void startWorkThread(String thread_name) {
        this.mBackgroundThread = new HandlerThread(thread_name);
        this.mBackgroundThread.start();
        this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
    }
    /**
     * 安全停止后台线程和对应的Handler
     */
    public void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
        }
        try {
            if (mBackgroundThread != null) {
                mBackgroundThread.join();
                mBackgroundThread = null;
            }
            if (mBackgroundHandler != null) {
                mBackgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public HandlerThread getBackgroundThread() {
        return mBackgroundThread;
    }

    public Handler getBackgroundHandler() {
        return mBackgroundHandler;
    }
}
