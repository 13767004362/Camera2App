package com.xingen.camera.utils.animator;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.View;

/**
 * Created by ${xingen} on 2017/10/24.
 */

public class AnimatorBuilder {
    /**
     * 创建周期性的闪现动画
     * @param view
     * @return
     */
    public static Animator createFlashAnimator(View view){
        ObjectAnimator objectAnimator=ObjectAnimator.ofFloat(view,"alpha",1.0f,0.1f);
        objectAnimator.setDuration(900);
        //反向效果，重复的时候
      //  objectAnimator.setRepeatMode(ObjectAnimator.RESTART);
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        return  objectAnimator;
    }
}
