package com.xingen.camera.view.widget;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.xingen.camera.R;
import com.xingen.camera.utils.DisplayUtils;
import com.xingen.camera.utils.view.ViewUtils;

/**
 * Created by ${xinGen} on 2017/10/23.
 *
 * <p>
 * 一个竖直拖动的进度布局
 */

public class VerticalProgressBarLayout extends ConstraintLayout implements View.OnClickListener, View.OnTouchListener {
    private final String TAG=VerticalProgressBarLayout.class.getSimpleName();
    /**
     * Padding的距离2dp
     */
    private final int PADDING = 2;
    private final int[] viewId = new int[2];

    public VerticalProgressBarLayout(Context context) {
        super(context);
        initConfig();
    }

    public VerticalProgressBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initConfig();
        addChildView();
    }

    /**
     * 初始化配置
     */
    private void initConfig() {
        this.setBackgroundResource(R.drawable.camera_progress_bar_bg);
        int padding = DisplayUtils.dip2px(getContext().getApplicationContext(), PADDING);
        this.setPadding(padding, padding, padding, padding);
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    private ImageView add_btn, reduce_btn, touch_btn;

    /**
     * 添加子View
     */
    private void addChildView() {
        this.addView(createLineView());
        viewId[0] = ViewUtils.getViewId();
        this.add_btn = createAddImageView(viewId[0], R.drawable.camera_progress_bar_add_btn);
        this.addView(this.add_btn);
        viewId[1] = ViewUtils.getViewId();
        this.reduce_btn = createReduceImageView(viewId[1], R.drawable.camera_progress_bar_reduce_btn);
        this.addView(this.reduce_btn);
        this.touch_btn = createTouchImageView(ViewUtils.getViewId(), R.drawable.progress_bar_move);
        this.addView(touch_btn);
    }

    /**
     * 添加增加按钮
     *
     * @param id
     * @param imageId
     * @return
     */
    private ImageView createAddImageView(int id, int imageId) {
        ImageView imageView = createImageView(imageId);
        imageView.setId(id);
        LayoutParams layoutParams = createLayoutParams();
        layoutParams.topToTop = layoutParams.rightToRight = layoutParams.leftToLeft = LayoutParams.PARENT_ID;
        imageView.setLayoutParams(layoutParams);
        imageView.setOnClickListener(this);
        return imageView;
    }

    /**
     * 添加增加按钮
     *
     * @param id
     * @param imageId
     * @return
     */
    private ImageView createReduceImageView(int id, int imageId) {
        ImageView imageView = createImageView(imageId);
        imageView.setId(id);
        LayoutParams layoutParams = createLayoutParams();
        layoutParams.bottomToBottom = layoutParams.rightToRight = layoutParams.leftToLeft = LayoutParams.PARENT_ID;
        imageView.setLayoutParams(layoutParams);
        imageView.setOnClickListener(this);
        return imageView;
    }

    /**
     * 添加Move的按钮
     *
     * @param id
     * @param imageId
     * @return
     */
    private ImageView createTouchImageView(int id, int imageId) {
        ImageView imageView = createImageView(imageId);
        imageView.setId(id);
        LayoutParams layoutParams = createLayoutParams();
        layoutParams.bottomToTop = viewId[1];
        layoutParams.topToBottom = viewId[0];
        layoutParams.rightToRight = layoutParams.leftToLeft = LayoutParams.PARENT_ID;
        imageView.setLayoutParams(layoutParams);
        imageView.setOnTouchListener(this);
        return imageView;
    }

    /**
     * 添加竖直线
     *
     * @return
     */
    private ImageView createLineView() {
        ImageView imageView = new ImageView(getContext());
        imageView.setBackgroundResource(R.drawable.camera_progress_bar_line_iv);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, 0);
        layoutParams.rightToRight = layoutParams.leftToLeft
                = layoutParams.topToTop = layoutParams.bottomToBottom
                = LayoutParams.PARENT_ID;
        imageView.setLayoutParams(layoutParams);
        return imageView;
    }

    private ImageView createImageView(int imageResource) {
        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource(imageResource);
        return imageView;
    }

    private LayoutParams createLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onClick(View v) {
        //增加
        if (v.getId() == viewId[0]) {

        } //减少
        else if (v.getId() == viewId[1]) {

        }
    }
    /**
     * 移动可滑动的按钮
     * <p>
     * 范围从0~1
     */
    private void moveTouchBtn() {
        ConstraintLayout.LayoutParams layoutParams = (LayoutParams) touch_btn.getLayoutParams();
        layoutParams.verticalBias = calculationTouchVerticalBias(layoutParams.verticalBias,move_current_y - move_start_y);
        touch_btn.requestLayout();
    }
    /**
     * 计算 竖直方向的偏移量
     *
     * @param verticalBias 原本的顶部权重比例
     *  @param moveDistance  手势滑动距离
     */
    private float calculationTouchVerticalBias(float verticalBias,int moveDistance) {
        //分配到的高度范围
        int distributionHeight = getHeight() - add_btn.getHeight() - reduce_btn.getHeight();
        //控件的高度
        int viewHeight = touch_btn.getHeight();
        //除开控件高度的权重长度
        int weightDistance = distributionHeight - viewHeight;
        //移动前的顶部权重高度
        int top_weight_distance = (int) (verticalBias * weightDistance);
        //移动后的顶部权重高度
        int top_new_weight_distance = top_weight_distance + moveDistance;
        float verticalBias_new;
        if (top_new_weight_distance < 0) {
            verticalBias_new = 0;
        } else if (top_new_weight_distance >= weightDistance) {
            verticalBias_new = 1;
        } else {
            verticalBias_new = (float) top_new_weight_distance / weightDistance;
        }
        Log.i(TAG,"计算出来的顶部权重： "+verticalBias_new+" 先前的:"+verticalBias);
        return verticalBias_new;
    }
    private int move_start_y, move_current_y;
    private int mScaledTouchSlop;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                move_start_y = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                move_current_y = (int) event.getY();
                 if (Math.abs(move_current_y - move_start_y)>mScaledTouchSlop){
                     moveTouchBtn();
                 }
                break;
            case MotionEvent.ACTION_UP:
                move_start_y = move_current_y = 0;
                break;
            default:
                break;
        }
        return true;
    }
}
