package com.xingen.camera.base

import android.view.LayoutInflater
import android.view.View
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**
 * author：HeXinGen
 * date：on 2023/8/16
 * describe:
 *
 * 构建一个ViewBind的基础Activity类
 *
 */
abstract class BaseVBActivity<VB:ViewBinding>:BaseActivity() {

    private var _binding:VB?=null
    protected val binding get()=_binding!!


    override fun createRootView(): View {
        generateViewBinding()
        return binding.root
    }
    /**
     * 生成ViewBinding
     */
    private fun generateViewBinding() {
        val type = this::class.java.genericSuperclass as? ParameterizedType ?: return
        val vbClazz = type.actualTypeArguments.find {
            // 找到泛型声明为 实现了 ViewBinding接口的类型
            (it as Class<*>).genericInterfaces[0] == ViewBinding::class.java
        } as? Class<*> ?: return
        val method = vbClazz.getMethod("inflate", LayoutInflater::class.java)
        _binding = method.invoke(null, layoutInflater) as VB
    }

    override fun getLayoutId(): Int=0
}