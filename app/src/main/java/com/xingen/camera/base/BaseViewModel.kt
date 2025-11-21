package com.xingen.camera.base

import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author: HeXinGen
 * @date: 2023/10/27
 * @descption:
 *
 * 基础viewmodel
 * 1.使用图示样式
 */
open class BaseViewModel: ViewModel(){

    var  toast_event= MutableLiveData<ToastEvent>()

    var loading_event=MutableLiveData<LoadingEvent>()

    data class LoadingEvent(var show:Boolean)

    /**
     * @param type 0:正常 1:带勾 2:带×
     */
    data class ToastEvent(var type:Int ,var content:String)

    var load_start_time=0L
    fun  postLoadingShowEvent(){
        load_start_time=System.currentTimeMillis()
        val event=LoadingEvent(true)
        if (isMainThread()){
            loading_event.value=event
        }else{
            loading_event.postValue(event)
        }
    }

    fun postLoadingHideEvent(){
        postFairLoadingHideEvent(false)
    }
    fun postFairLoadingHideEvent(fair:Boolean){
        viewModelScope.launch {
            var currentTime=System.currentTimeMillis()
            val interval=currentTime-load_start_time
            if (interval<1000){
                //间隔时间不得小于一秒,防止造成一闪一闪
             //   LogUtils.printI("postLoadingHideEvent","delay $interval")
                delay(interval)
            }
          //  LogUtils.printI("postLoadingHideEvent","do hide dialog task")
            val event=LoadingEvent(false)
            if (isMainThread()){
                loading_event.value=event
            }else{
                loading_event.postValue(event)
            }
        }

    }

    /**
     * 是否是主线程
     */
    fun  isMainThread():Boolean{
        return Looper.myLooper()== Looper.getMainLooper()
    }

    /**
     * 传递异常的提示Toast
     */
    fun postErrorToast(content: String){
        toast_event.postValue(ToastEvent(2,content))
    }

    fun postSuccessToast(content: String){
        toast_event.postValue(ToastEvent(1,content))
    }
}