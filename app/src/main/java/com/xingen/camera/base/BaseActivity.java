package com.xingen.camera.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xingen.camera.view.systembar.SystemBarUtils;

/**
 * Created by ${xingen} on 2017/10/20.
 */

public  abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        setSystemUIChangeListener();
        initView(savedInstanceState);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //焦点改变的时候，当Home键退出，重新从新进入等情况的处理。
        SystemBarUtils.setStickyStyle(getWindow());
    }

    /**
     * 监听系统UI的显示，进行特殊处理
     */
    private void setSystemUIChangeListener() {
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            //当系统UI显示的时候（例如输入法显示的时候），再次隐藏
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                SystemBarUtils.setStickyStyle(getWindow());
            }
        });
    }

    /**
     *  获取布局的Id
     * @return
     */
    protected abstract int getLayoutId();

    /**
     * 初始化
     * @param savedInstanceState
     */

    protected abstract void initView(Bundle savedInstanceState);

}
