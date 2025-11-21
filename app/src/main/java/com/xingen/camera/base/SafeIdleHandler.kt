package com.xingen.camera.base

import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import android.os.MessageQueue.IdleHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.CopyOnWriteArrayList

/**
 * author：HeXinGen
 * date：on 2023/7/1
 * describe:
 *
 * 1.结合lifecycle ,在销毁时，进行消息移除
 * 2.结合空闲时的任务
 */
class SafeIdleHandler(looper: Looper, lifecycle: Lifecycle) : Handler(looper) {
    init {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    //释放资源
                    this@SafeIdleHandler.removeCallbacksAndMessages(null)
                    messageQueue?.removeIdleHandler(this@SafeIdleHandler.idleHandler)
                    taskQueue.clear()
                }
            }
        })
    }


    val idleHandler = IdleHandler {
        if (taskQueue.isNotEmpty()) {
            for (task in taskQueue) {
                try {
                    task.run()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            taskQueue.clear()
        }
        false // 若是返回false,会自动移除;若是返回true:下次空闲时，仍然会检查执行
    }
    private var messageQueue: MessageQueue? = null
    private var taskQueue = CopyOnWriteArrayList<Runnable>()

    /**
     * 不阻塞UI 线程, 利用handle Idle 机制
     * @param task 闲时任务
     */
    fun addIdleTask(task: Runnable) {
        taskQueue.add(task)
        if (messageQueue == null) {
            messageQueue = Looper.myQueue().also {
                it.addIdleHandler(this.idleHandler)
            }
        }
    }
    fun execute(task: Runnable){
        if(isMainThread()){
            task.run()
        }else{
            this.post(task)
        }
    }

    companion object {
        /**
         * 是否为主要线程
         */
        fun isMainThread(): Boolean {
            return Looper.getMainLooper() == Looper.myLooper()
        }
    }
}