package com.xingen.camera.base

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity


/**
 * author：HeXinGen
 * date：on 2023/7/1
 * describe:
 *
 * 构建一个通用的父类
 */
abstract class BaseActivity : AppCompatActivity() {
    protected val mainHandler: SafeIdleHandler =
        SafeIdleHandler(Looper.getMainLooper(), this.lifecycle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createRootView()?.run {
            setContentView(this)
        }


        setStatusBar()
        initConfig(savedInstanceState)
        //采用延迟初始化UI,提高Activity启动速度。
        mainHandler.post {
            initView(savedInstanceState)
        }
    }

    /**
     * 构建一个根布局的rootView
     */
    protected open fun createRootView(): View? {

        return LayoutInflater.from(this).inflate(getLayoutId(), null)
    }

    /**
     * 覆盖重写可，设置layout对应的布局id
     */
    protected open fun getLayoutId(): Int {
        return 0
    }

    protected open fun initConfig(savedInstanceState: Bundle?) {
        initArgs(intent.extras, savedInstanceState)
    }

    /**
     * @param args 打开activity传递的参数
     * @param savedInstanceState 不为null,系统回收重新创建Actvity
     */
    protected open fun initArgs(args: Bundle?, savedInstanceState: Bundle?) {

    }

    /**
     * 用于各个子类设置statusbar
     */
    protected open fun setStatusBar() {

    }

    /**
     * 可用于添加空闲任务
     */
    protected fun addIdleTask(task: Runnable) {
        mainHandler.addIdleTask(task)
    }

    protected abstract fun initView(savedInstanceState: Bundle?)


}