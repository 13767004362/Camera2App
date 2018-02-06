package com.xingen.camera.presenter;

import android.content.Context;
import android.content.pm.PackageManager;

import com.xingen.camera.base.BaseApplication;
import com.xingen.camera.camera2.BaseCamera2Operator;
import com.xingen.camera.camera2.Camera2Manager;
import com.xingen.camera.contract.CameraContract;
import com.xingen.camera.mode.Constant;
import com.xingen.camera.utils.permissions.PermissionsManager;
import com.xingen.camera.utils.thread.WorkThreadUtils;
import com.xingen.camera.utils.time.TimingUtils;
import com.xingen.camera.utils.toast.ToastUtils;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by ${xinGen} on 2017/10/19.
 */
public class CameraPresenter implements CameraContract.Presenter, BaseCamera2Operator.Camera2ResultCallBack, BaseCamera2Operator.Camera2VideoRecordCallBack {
    private final String TAG=CameraPresenter.class.getSimpleName();
    private WorkThreadUtils workThreadManager;
    private Camera2Manager camera2Manager;
    private CameraContract.View view;
    private CompositeSubscription compositeSubscription;
    private Context appContext;
    private int currentMode;

    public CameraPresenter(CameraContract.View view) {
        this.view = view;
        this.view.setPresenter(this);
        this.compositeSubscription = new CompositeSubscription();
        this.workThreadManager = WorkThreadUtils.newInstance();
        this.appContext = BaseApplication.getInstance();
        this.camera2Manager = new Camera2Manager(appContext, this.workThreadManager);
        this.camera2Manager.setCamera2ResultCallBack(this);
        this.camera2Manager.setCameraVideoCallBack(this);
        this.currentMode = Constant.MODE_CAMERA;
    }

    @Override
    public void switchMode(int mode) {
        if (mode == currentMode) {
            return;
        }
        currentMode = mode;
        switch (currentMode) {
            //切换到拍照模式
            case Constant.MODE_CAMERA:
                break;
            //切换到录像模式
            case Constant.MODE_VIDEO_RECORD:
                break;
            default:
                break;
        }
        this.camera2Manager.switchMode(currentMode);
    }

    @Override
    public void switchCamera(int direction) {
       this.camera2Manager.switchCameraDirection(direction);
    }

    @Override
    public void onResume() {
        this.workThreadManager.startWorkThread();
        this.camera2Manager.onResume(view.getCameraView());
    }
    @Override
    public void onPause() {
        this.camera2Manager.onPause();
        this.workThreadManager.stopBackgroundThread();
    }
    @Override
    public void takePictureOrVideo() {
        this.camera2Manager.takePictureOrVideo();
    }
    @Override
    public void callBack(Observable<String> result) {
        if (result != null) {
            Subscription subscription = result.subscribeOn(Schedulers.io())
                    .unsubscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(filePath ->
                                    view.loadPictureResult(filePath)
                            , throwable -> {
                                //写入图片到磁盘失败
                                ToastUtils.showToast(BaseApplication.getInstance(), "写入磁盘失败");
                            });
            this.compositeSubscription.add(subscription);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PermissionsManager.CAMERA_REQUEST_CODE:
                //权限请求失败
                if (grantResults.length == PermissionsManager.CAMERA_REQUEST.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            ToastUtils.showToast(BaseApplication.getInstance(), "拍照权限被拒绝");
                            break;
                        }
                    }
                }
                break;
            case PermissionsManager.VIDEO_REQUEST_CODE:
                if (grantResults.length == PermissionsManager.VIDEO_PERMISSIONS.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            ToastUtils.showToast(BaseApplication.getInstance(), "录像权限被拒绝");
                            break;
                        }
                    }
                }
                break;
            default:
                break;
        }
    }
    private long time = 0;
    private Subscription cycleTimeSubscription;
    @Override
    public void stopRecord() {
        if (cycleTimeSubscription != null) {
            this.compositeSubscription.remove(cycleTimeSubscription);
        }
        this.view.switchRecordMode(CameraContract.View.MODE_RECORD_STOP);
        this.camera2Manager.pauseVideoRecord();
    }

    @Override
    public void restartRecord() {
        this.camera2Manager.takePictureOrVideo();
    }

    @Override
    public void setManualFocus(float focusProportion) {
       camera2Manager.setZoomProportion(focusProportion);
    }
    @Override
    public void finishRecord() {
        this.view.switchRecordMode(CameraContract.View.MODE_RECORD_FINISH);
        if (cycleTimeSubscription != null) {
            this.compositeSubscription.remove(cycleTimeSubscription);
        }
        time = 0;
    }
    @Override
    public void startRecord() {
        this.view.switchRecordMode(CameraContract.View.MODE_RECORD_START);
        cycleTimeSubscription = Observable.interval(1, TimeUnit.SECONDS, Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.computation())
                .subscribe(s -> {
                            time += 1000;
                            String time_show = TimingUtils.getDate(time);
                            view.setTimingShow(time_show);
                        }
                );
        this.compositeSubscription.add(cycleTimeSubscription);
    }

}
