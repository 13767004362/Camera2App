package com.xingen.camera.utils.toast;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

/**
 * Created by ${xingen} on 2017/10/21.
 * <p>
 * 一个Toast工具类
 */

public class ToastUtils {
    /**
     * 切换到UI线程显示Toast
     *
     * @param context
     * @param content
     */
    public static void showToastRunUIThread(Context context, String content) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() ->
                    showToast(context, content)
            );
        } else {
            showToast(context, content);
        }
    }

    /**
     * 显示Toast
     * @param context
     * @param content
     */
    public static void showToast(Context context, String content) {
        Toast.makeText(context.getApplicationContext(), content, Toast.LENGTH_SHORT).show();
    }
}
