package com.xingen.camera.base

import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.xingen.camera.utils.GenericClassUtils

/**
 * @author: HeXinGen
 * @date: 2023/10/17
 * @descption:
 *
 * 业务的基础Activity: mvvm
 */
abstract class BaseVMActivity<VB : ViewBinding, T : BaseViewModel> : BaseVBActivity<VB>() {
    protected lateinit var viewModel: T
    protected lateinit var viewModelProvider: ViewModelProvider

    override fun initView(savedInstanceState: Bundle?) {
        initViewModel()
        initViewImpl(savedInstanceState)
    }


    /**
     * 点击任意地方，可关闭输入法
     */
    protected open fun inputCloseClickAnyEvent() = false

    /**
     * 软键盘关闭，自动清除焦点
     */
    protected open fun keyBoardCloseClearFocus() = false

    /**
     * 初始化ViewModel
     */
    private fun initViewModel() {
        viewModelProvider = ViewModelProvider(this)
        val mClassArray = GenericClassUtils.queryGenericClass(this.javaClass)
        val modelClass = mClassArray[1] as Class<T>
        viewModel = createViewModel(modelClass)
        subViewModel()
    }

    /**
     * 创建ViewModel
     * 一个view 可能对应多个ViewModel
     * 1.若是baseViewModel的子类会默认绑定toast和dialog事件
     */
    fun <V : ViewModel> createViewModel(modelClass: Class<V>): V {
        var targetViewModel = viewModelProvider.get(modelClass)
        if (targetViewModel is BaseViewModel) {
            observeCommEvent(targetViewModel)
        }
        return targetViewModel;
    }

    protected fun observeCommEvent(targetViewModel: BaseViewModel) {
        targetViewModel.toast_event.observe(this) {
        }
        targetViewModel.loading_event.observe(this) {
            if (it.show) {
                showLoadingDialog()
            } else {
                hideLoadingDialog()
            }
        }
    }

    fun hideLoadingDialog() {

    }

    fun showLoadingDialog() {

    }

    /**
     * 初始化UI
     */
    abstract open fun initViewImpl(savedInstanceState: Bundle?)

    /**
     * 订阅ViewModel
     */
    protected abstract open fun subViewModel()

    /**
     * 显示异常消息
     */
    fun postErrorToast(content: String) {
        viewModel.toast_event.value = BaseViewModel.ToastEvent(2, content)
    }

    /**
     * 处理startActivityForResult的结果
     */
    protected open fun handleActivityResult(activityResult: ActivityResult) {
        if (activityResult.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    /**
     * 使用ActivityResult API
     */
    protected val activityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handleActivityResult(it)
        }
}