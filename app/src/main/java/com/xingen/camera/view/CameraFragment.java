package com.xingen.camera.view;

import android.animation.Animator;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.xingen.camera.PictureActivity;
import com.xingen.camera.R;
import com.xingen.camera.camera2.Camera2Manager;
import com.xingen.camera.contract.CameraContract;
import com.xingen.camera.glide.GlideLoader;
import com.xingen.camera.mode.Constant;
import com.xingen.camera.utils.animator.AnimatorBuilder;
import com.xingen.camera.widget.AutoFitTextureView;

/**
 * Created by ${xinGen} on 2017/10/19.
 */

public class CameraFragment extends Fragment implements CameraContract.View<CameraContract.Presenter>, View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    public static final String TAG = CameraFragment.class.getSimpleName();

    public static CameraFragment newInstance() {
        CameraFragment cameraFragment = new CameraFragment();
        return cameraFragment;
    }

    private AutoFitTextureView textureView;
    private TextView show_record_tv, record_tip_circle;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        initView();
        return this.rootView;
    }

    private ImageView show_result_iv;
    private ImageView controller_state_iv;

    private void initView() {
        this.textureView = rootView.findViewById(R.id.camera_auto_fit_texture_view);
        this.show_result_iv = rootView.findViewById(R.id.camera_show);
        this.show_record_tv = rootView.findViewById(R.id.camera_video_record_tip_time_tv);
        this.record_tip_circle = rootView.findViewById(R.id.camera_video_record_tip_bg);
        this.rootView.findViewById(R.id.camera_btn).setOnClickListener(this);
        this.controller_state_iv = this.rootView.findViewById(R.id.camera_right_top_controller);
        this.controller_state_iv.setTag(CameraContract.View.MODE_RECORD_FINISH);
        this.controller_state_iv.setOnClickListener(this);
        this.show_result_iv.setOnClickListener(this);
        ((RadioGroup) this.rootView.findViewById(R.id.camera_switch_radioGroup)).setOnCheckedChangeListener(this);
        ((RadioGroup) this.rootView.findViewById(R.id.camera_direction_radioGroup)).setOnCheckedChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (presenter != null) {
            presenter.onPause();
        }
    }

    private CameraContract.Presenter presenter;

    @Override
    public void setPresenter(CameraContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public TextureView getCameraView() {
        return textureView;
    }
    protected  String filePath;
    @Override
    public void loadPictureResult(String filePath) {
        this.filePath=filePath;
        GlideLoader.loadNetWorkResource(getActivity(), filePath, show_result_iv);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //点击按钮，进行拍照或录像
            case R.id.camera_btn:
                this.presenter.takePictureOrVideo();
                break;
            // 控制视频录制的开关,包含暂停，恢复录制
            case R.id.camera_right_top_controller:
                int mode = (int) controller_state_iv.getTag();
                //录制状态中，可以暂停
                if (mode == CameraContract.View.MODE_RECORD_START) {
                    this.presenter.stopRecord();
                }//暂停状态，可以继续开始录制
                else if (mode == CameraContract.View.MODE_RECORD_STOP) {
                    this.presenter.restartRecord();
                }
                break;
            //查看大图
            case R.id.camera_show:
                if (!TextUtils.isEmpty(filePath)){
                    PictureActivity.openActivity(getActivity(),filePath);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void setTimingShow(String timing) {
        this.show_record_tv.setText(timing);
    }

    private Animator flashAnimator;

    @Override
    public void switchRecordMode(int mode) {
        switch (mode) {
            //录制开始
            case CameraContract.View.MODE_RECORD_START:
                this.show_record_tv.setVisibility(View.VISIBLE);
                this.record_tip_circle.setVisibility(View.VISIBLE);
                this.controller_state_iv.setImageResource(R.drawable.camera_stop_iv);
                if (flashAnimator != null && flashAnimator.isRunning()) {
                    flashAnimator.cancel();
                }
                break;
            //录制暂停
            case CameraContract.View.MODE_RECORD_STOP:
                this.controller_state_iv.setImageResource(R.drawable.camera_start_iv);
                this.show_record_tv.setVisibility(View.INVISIBLE);
                flashAnimator = AnimatorBuilder.createFlashAnimator(this.record_tip_circle);
                flashAnimator.start();
                break;
            //录制完成
            case CameraContract.View.MODE_RECORD_FINISH:
                this.show_record_tv.setVisibility(View.GONE);
                this.record_tip_circle.setVisibility(View.GONE);
                this.controller_state_iv.setImageResource(R.drawable.camera_init_iv);
                break;
            default:
                break;
        }
        this.controller_state_iv.setTag(mode);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            //切换成录像模式
            case R.id.camera_video_record_btn:
                this.presenter.switchMode(Constant.MODE_VIDEO_RECORD);
                break;
            //切换到拍照模式
            case R.id.camera_switch_picture_btn:
                this.presenter.switchMode(Constant.MODE_CAMERA);
                break;
            //切换到前摄像头
            case R.id.camera_direction_front:
                this.presenter.switchCamera(Camera2Manager.CAMERA_DIRECTION_FRONT);
                break;
            //切换到后摄像头
            case R.id.camera_direction_back:
                this.presenter.switchCamera(Camera2Manager.CAMERA_DIRECTION_BACK);
                break;
            default:

                break;
        }
    }
}
