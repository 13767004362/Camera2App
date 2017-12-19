package com.xingen.camera.utils.view;

import android.graphics.Paint;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ${xingen} on 2017/6/20.
 */

public class ViewUtils {
    /**
     * 随机分配一个新的Id
     *
     * @return
     */
    public static int getViewId() {
        int viewId;
        if (Build.VERSION.SDK_INT < 17) {
            //采用View中源码
            viewId = generateViewId();
        } else {
            //采用View中generateViewId()静态方法
            viewId = View.generateViewId();
        }
        return viewId;
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * Generate a value suitable for use in {@link #setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    private static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }
    /**
     * 计算字体的高度
     *
     * @param paint
     * @return
     */
    public  static float calculationDigitalHeight(Paint paint) {
        return ((-paint.ascent()) + paint.descent());
    }
    /**
     *  计算字体的长度
     * @param paint
     * @param text
     * @return
     */
    public static float calculationDigitalWith(Paint paint, String text) {
        return paint.measureText(text);
    }

    /**
     * 检查文本是否为空
     * @param msg
     * @return
     */
    public static boolean isNull(String msg){
        return TextUtils.isEmpty(msg);
    }

    /**
     *  反射获取状态栏高度
     * @return
     */
    public static int getStatusBarHeight(){
        int x=0;
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object o = c.newInstance();
            Field field = c.getField("status_bar_height");
             x = (Integer) field.get(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  x;
    }

}
