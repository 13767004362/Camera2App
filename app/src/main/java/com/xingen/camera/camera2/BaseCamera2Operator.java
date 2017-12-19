package com.xingen.camera.camera2;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import com.xingen.camera.base.BaseApplication;

import java.lang.ref.WeakReference;
import java.util.concurrent.Semaphore;

import rx.Observable;

/**
 * Created by ${xingen} on 2017/10/21.
 * <p>
 * 一个超类，抽出一些共同的行为，在拍照和录像中。
 */

public abstract class BaseCamera2Operator {
    /**
     * 用于防止应用程序退出前 ，关闭相机
     */
    protected Semaphore mCameraOpenCloseLock = new Semaphore(1);
    /**
     * 相机设备
     */
    protected CameraDevice mCameraDevice;
    /**
     * App的生命周期
     */
    protected Context appContext;
    /**
     * 转换屏幕旋转角度到JPEG的方向
     */
    protected static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    protected static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    protected static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    protected static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();

    protected int currentDirection;
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private WeakReference<TextureView> weakReference;
    public BaseCamera2Operator() {

    }
    protected void setWeakReference(TextureView textureView) {
        if (getTextureView()==null){
            this.weakReference = new WeakReference<>(textureView);
            this.appContext= BaseApplication.getInstance();
        }
    }

    protected TextureView getTextureView() {
        return weakReference != null ? weakReference.get() : null;
    }



    protected Activity getTextureViewContext() {
        return (Activity) getTextureView().getContext();
    }

    public TextureView.SurfaceTextureListener getmSurfaceTextureListener() {
        return mSurfaceTextureListener;
    }
    public void setCurrentDirection(int currentDirection) {
        this.currentDirection = currentDirection;
    }

    /**
     * 切换摄像头的操作
     */
    public   void switchCameraDirectionOperate(){
              this.stopOperate();
              this.startOperate();
    }
    /**
     * TextureView的生命周期，事件回调。
     */
    protected final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(getTextureViewContext(), width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(getTextureViewContext(), width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    /**
     * 当相机设备的状态发生改变的时候，将会回调。
     */
    protected final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        /**
         * 当相机打开的时候，调用
         * @param cameraDevice
         */
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // 释放掉那个锁
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            startPreView();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        /**
         * 发生异常的时候调用
         *
         * 这里释放资源，然后关闭界面
         * @param cameraDevice
         * @param error
         */
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getTextureViewContext();
            if (null != activity) {
                activity.finish();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
        }
    };
    /**
     * ImageReader的回调监听器
     * <p>
     * onImageAvailable被调用的时候，已经拍照完，准备保存的操作
     * 通常写入磁盘文件中。
     */
    protected final ImageReader.OnImageAvailableListener onImageAvailableListener = (ImageReader reader)
            -> writePictureData(reader.acquireNextImage());

    /**
     * 写入图片数据
     *
     * @param image
     */
   protected abstract void writePictureData(Image image);

    /**
     * 开始时预览，更新请求
     */
   protected abstract void startPreView();

    /**
     *相机按钮的点击事件，可能在拍照，可能在录像
     */
    public abstract void cameraClick();

    /**
     * 开始操作
     */
    public abstract  void startOperate();

    /**
     * 关闭当前的PreviewSession
     */
   protected abstract  void closePreviewSession();

    /**
     * 停止操作
     */
    public  abstract  void stopOperate();


    /**
     * 获取到Id，然后通过指定Id打开指定的相机。
     *
     * 结果将会StateCallback监听器中回调。
     *
     * @param  activity
     * @param width
     * @param height
     */
    protected abstract void openCamera(Activity activity, int width, int height);
    /**
     * 配置需要转换的Matrix到TextureView中。
     * 在设置完相机预览大小后，才调用
     * @param  activity
     * @param viewWidth
     * @param viewHeight
     */
   protected abstract void configureTransform(Activity activity, int viewWidth, int viewHeight);

    public interface Camera2ResultCallBack {
        /**
         * 写入JPEG图片后返回的路径
         *
         * @param result
         */
        void callBack(Observable<String> result);
    }
    protected Camera2ResultCallBack camera2ResultCallBack;

    public void setCamera2ResultCallBack(Camera2ResultCallBack camera2ResultCallBack) {
        this.camera2ResultCallBack = camera2ResultCallBack;
    }
    public interface  Camera2VideoRecordCallBack{
        /**
         *  开始录制
         */
        void startRecord();

        /**
         * 完成录制
         */
        void finishRecord();
    }
}
