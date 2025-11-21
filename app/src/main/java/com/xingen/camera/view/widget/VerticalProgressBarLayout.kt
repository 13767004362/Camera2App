package com.xingen.camera.view.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.xingen.camera.R
import com.xingen.camera.utils.DisplayUtils
import com.xingen.camera.utils.view.ViewUtils
import kotlin.math.abs

/**
 * Created by ${xinGen} on 2017/10/23.
 *
 *
 *
 * 一个竖直拖动的进度布局
 *
 * 采用Java代码手写一个动态布局。
 *
 */
class VerticalProgressBarLayout : FrameLayout, View.OnClickListener, OnTouchListener {
    private val TAG: String = VerticalProgressBarLayout::class.java.getSimpleName()

    /**
     * Padding的距离2dp
     */
    private val PADDING = 2
    private val viewId = IntArray(2)
    private var add_btn: ImageView? = null
    private var reduce_btn: ImageView? = null
    private var touch_btn: ImageView? = null
    var verticalMoveResultListener: VerticalMoveResultListener? = null
    private var layoutParams: ConstraintLayout.LayoutParams? = null

    constructor(context: Context) : super(context) {
        initConfig()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initConfig()
        addChildView()
    }

    /**
     * 初始化配置
     */
    private fun initConfig() {
        mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    /**
     * 添加子View
     */
    private fun addChildView() {
        setBackgroundResource(R.drawable.camera_progress_bar_bg)
        val padding = DisplayUtils.dip2px(getContext().getApplicationContext(), PADDING.toFloat())
        setPadding(padding, padding, padding, padding)
        this.addView(createLineView())

        val childLayout = ConstraintLayout(getContext())
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        childLayout.setLayoutParams(layoutParams)
        viewId[0] = ViewUtils.getViewId()
        this.add_btn = createAddImageView(viewId[0], R.drawable.camera_progress_bar_add_btn)
        childLayout.addView(this.add_btn)
        viewId[1] = ViewUtils.getViewId()
        this.reduce_btn =
            createReduceImageView(viewId[1], R.drawable.camera_progress_bar_reduce_btn)
        childLayout.addView(this.reduce_btn)
        this.touch_btn = createTouchImageView(ViewUtils.getViewId(), R.drawable.progress_bar_move)
        childLayout.addView(touch_btn)
        this.layoutParams = touch_btn!!.getLayoutParams() as ConstraintLayout.LayoutParams
        this.addView(childLayout)
    }

    /**
     * 添加增加按钮
     *
     * @param id
     * @param imageId
     * @return
     */
    private fun createAddImageView(id: Int, imageId: Int): ImageView {
        val imageView = createImageView(imageId)
        imageView.setId(id)
        val layoutParams = createLayoutParams()
        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.rightToRight = layoutParams.leftToLeft
        layoutParams.topToTop = layoutParams.rightToRight
        imageView.setLayoutParams(layoutParams)
        imageView.setOnClickListener(this)
        return imageView
    }

    /**
     * 添加增加按钮
     *
     * @param id
     * @param imageId
     * @return
     */
    private fun createReduceImageView(id: Int, imageId: Int): ImageView {
        val imageView = createImageView(imageId)
        imageView.setId(id)
        val layoutParams = createLayoutParams()
        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.rightToRight = layoutParams.leftToLeft
        layoutParams.bottomToBottom = layoutParams.rightToRight
        imageView.setLayoutParams(layoutParams)
        imageView.setOnClickListener(this)
        return imageView
    }

    /**
     * 添加Move的按钮
     *
     * @param id
     * @param imageId
     * @return
     */
    private fun createTouchImageView(id: Int, imageId: Int): ImageView {
        val imageView = createImageView(imageId)
        imageView.setId(id)
        val layoutParams = createLayoutParams()
        layoutParams.bottomToTop = viewId[1]
        layoutParams.topToBottom = viewId[0]
        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.rightToRight = layoutParams.leftToLeft
        layoutParams.verticalBias = 1f
        imageView.setLayoutParams(layoutParams)
        imageView.setOnTouchListener(this)
        return imageView
    }

    /**
     * 添加竖直线
     *
     * @return
     */
    private fun createLineView(): TextView {
        val textView = TextView(context)
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        textView.setLayoutParams(layoutParams)
        textView.setBackgroundResource(R.drawable.camera_progress_bar_line_iv)
        return textView
    }

    private fun createImageView(imageResource: Int): ImageView {
        val imageView = ImageView(context)
        imageView.setImageResource(imageResource)
        return imageView
    }

    private fun createLayoutParams(): ConstraintLayout.LayoutParams {
        return ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onClick(v: View) {
        var old: Float = layoutParams!!.verticalBias
        //增加
        if (v.id == viewId[0]) {
            if (old > 0) {
                old -= 0.1.toFloat()
                if (old < 0) {
                    old = 0f
                }
                moveTouchBtn(old)
            }
        } //减少
        else if (v.id == viewId[1]) {
            if (old < 1) {
                old += 0.1.toFloat()
                if (old > 1) {
                    old = 1f
                }
                moveTouchBtn(old)
            }
        }
    }

    /**
     * 移动可滑动的按钮
     *
     *
     * 范围从0~1
     */
    private fun moveTouchBtn(verticalBias: Float) {
         setMoveVerticalBias(verticalBias)
        if (this.verticalMoveResultListener != null) {
            this.verticalMoveResultListener!!.moveDistance(1 - layoutParams!!.verticalBias)
        }
    }

    fun setMoveVerticalBias(verticalBias: Float) {
        if (layoutParams!!.verticalBias == verticalBias) {
            return
        }
        layoutParams!!.verticalBias = verticalBias
        touch_btn!!.requestLayout()
    }

    /**
     * 计算 竖直方向的偏移量
     *
     * @param verticalBias 原本的顶部权重比例
     * @param moveDistance  手势滑动距离
     */
    private fun calculationTouchVerticalBias(verticalBias: Float, moveDistance: Int): Float {
        //分配到的高度范围
        val distributionHeight = getHeight() - add_btn!!.getHeight() - reduce_btn!!.getHeight()
        //控件的高度
        val viewHeight = touch_btn!!.getHeight()
        //除开控件高度的权重长度
        val weightDistance = distributionHeight - viewHeight
        //移动前的顶部权重高度
        val top_weight_distance = (verticalBias * weightDistance).toInt()
        //移动后的顶部权重高度
        val top_new_weight_distance = top_weight_distance + moveDistance
        val verticalBias_new: Float
        if (top_new_weight_distance < 0) {
            verticalBias_new = 0f
        } else if (top_new_weight_distance >= weightDistance) {
            verticalBias_new = 1f
        } else {
            verticalBias_new = top_new_weight_distance.toFloat() / weightDistance
        }
         Log.i(TAG,"计算出来的顶部权重： "+verticalBias_new+" 先前的:"+verticalBias);
        return verticalBias_new
    }

    private var move_start_y = 0
    private var move_current_y = 0
    private var mScaledTouchSlop = 0
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> move_start_y = event.y.toInt()
            MotionEvent.ACTION_MOVE -> {
                move_current_y = event.y.toInt()
                if (abs(move_current_y - move_start_y) > mScaledTouchSlop) {
                    val verticalBias = calculationTouchVerticalBias(
                        layoutParams!!.verticalBias,
                        move_current_y - move_start_y
                    )
                    moveTouchBtn(verticalBias)
                }
            }

            MotionEvent.ACTION_UP -> {
                move_current_y = 0
                move_start_y = move_current_y
            }

            else -> {}
        }
        return true
    }

    interface VerticalMoveResultListener {
        fun moveDistance(verticalBias: Float)
    }
}
