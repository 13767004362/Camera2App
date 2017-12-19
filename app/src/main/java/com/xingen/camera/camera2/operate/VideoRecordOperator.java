package com.xingen.camera.camera2.operate;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.xingen.camera.camera2.BaseCamera2Operator;
import com.xingen.camera.camera2.calculation.Camera2CalculationUtils;
import com.xingen.camera.utils.file.FileUtils;
import com.xingen.camera.utils.permissions.PermissionsManager;
import com.xingen.camera.utils.rxjava.ObservableBuilder;
import com.xingen.camera.utils.thread.WorkThreadUtils;
import com.xingen.camera.utils.toast.ToastUtils;
import com.xingen.camera.widget.AutoFitTextureView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by ${xinGen} on 2017/10/21.
 * <p>
 * 录像操作类
 */

public class VideoRecordOperator extends BaseCamera2Operator {
    public static final String TAG = VideoRecordOperator.class.getSimpleName();
    private WorkThreadUtils workThreadManager;

    /**
     * 视频录制的大小
     */
    private Size mVideoSize;
    /**
     * 相机预览的大小Size
     */
    private Size mPreviewSize;
    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;
    /**
     * 当前是否是在录制视频
     */
    private boolean mIsRecordingVideo;
    /**
     * 传感器的方向
     */
    private Integer mSensorOrientation;
    /**
     * 相机预览请求的Builder
     */
    private CaptureRequest.Builder mPreviewBuilder;
    /**
     * 点击开启录制时候创建的新视频文件路径
     */
    private String mNextVideoAbsolutePath;
    private CompositeSubscription compositeSubscription;

    public VideoRecordOperator(WorkThreadUtils workThreadUtils) {
        this.workThreadManager = workThreadUtils;
        this.oldVideoPath = new ArrayList<>();
        this.compositeSubscription = new CompositeSubscription();
    }

    @Override
    public void writePictureData(Image image) {

    }

    @Override
    public void startPreView() {
        TextureView mTextureView = getTextureView();
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        //开始相机预览
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getTextureViewContext();
                            if (null != activity) {
                                Toast.makeText(activity, "相机预览配置失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (null != mTextureView) {
            configureTransform(getTextureViewContext(), mTextureView.getWidth(), mTextureView.getHeight());
        }
    }

    /**
     * 在 startPreView()之后执行用于更新相机预览界面
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
     /*       HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();*/
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * 为相机创建一个CameraCaptureSession
     */
    private CameraCaptureSession mPreviewSession;

    @Override
    public void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }


    @Override


    public void cameraClick() {
        if (mIsRecordingVideo) {
            stopRecordingVideo(true);
        } else {
            startRecordingVideo();
        }
    }
    @Override
    public void switchCameraDirectionOperate() {

    }

    /**
     * 停止录制
     */
    private void stopRecordingVideo(final  boolean isFinish) {

        // UI
        mIsRecordingVideo = false;
        /**
         * 在MediaRecorder停止前，停止相机预览，防止抛出serious error异常。
         *
         * android.hardware.camera2.CameraAccessException: The camera device has encountered a serious error
         *
         * 解决方式：https://stackoverflow.com/questions/27907090/android-camera-2-api
         */
        try {
            mPreviewSession.stopRepeating();
            mPreviewSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Subscription subscription = Observable
                .timer(30, TimeUnit.MICROSECONDS, Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> {
                    // 停止录制
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    if (isFinish) {
                        isRecordGonging=false;
                        Log.i(TAG, "stopRecordingVideo 录制完成");
                        if (camera2VideoRecordCallBack != null) {
                            camera2VideoRecordCallBack.finishRecord();
                        }
                        mergeMultipleFileCallBack();
                        mNextVideoAbsolutePath = null;
                        this.oldVideoPath.clear();
                    } else {//暂停的操作
                        Log.i(TAG, "pauseRecordingVideo 录制暂停");
                        //若是开始新的录制，原本暂停产生的多个文件合并成一个文件。
                        this.oldVideoPath.add(mNextVideoAbsolutePath);
                        if (oldVideoPath.size() > 1) {
                            mergeMultipleFile();
                        }
                        mNextVideoAbsolutePath = null;
                    }
                    startPreView();
                });
        this.compositeSubscription.add(subscription);
    }

    /**
     * 暂停后又从新恢复录制，合并多个视频文件
     */
    private void mergeMultipleFile() {
        Log.i(TAG, " mergeMultipleFile  开始操作：文件个数 " + this.oldVideoPath.size());
        Subscription subscription = ObservableBuilder.createMergeMuiltFile(appContext, this.oldVideoPath.get(0), this.oldVideoPath.get(1))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filePath -> {
                    this.oldVideoPath.clear();
                    this.oldVideoPath.add(filePath);
                    Log.i(TAG, " mergeMultipleFile  完成： 文件个数" + this.oldVideoPath.size());
                });
        this.compositeSubscription.add(subscription);
    }

    /**
     * 完成录制，输出最终的视频录制文件
     */
    private void mergeMultipleFileCallBack() {
        if (this.oldVideoPath.size() > 0) {
            Log.i(TAG, " mergeMultipleFileCallBack 开始操作：文件个数 " + this.oldVideoPath.size());
            Subscription subscription = ObservableBuilder.createMergeMuiltFile(appContext, this.oldVideoPath.get(0), mNextVideoAbsolutePath)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {
                        if (camera2ResultCallBack != null) {
                            camera2ResultCallBack.callBack(ObservableBuilder.createVideo(s));
                        }
                        Log.i(TAG, " mergeMultipleFileCallBack 完成 且回调");
                        ToastUtils.showToast(appContext, "视频文件保存在" + s);
                    });
            this.compositeSubscription.add(subscription);
        } else {
            if (camera2ResultCallBack != null) {
                camera2ResultCallBack.callBack(ObservableBuilder.createVideo(mNextVideoAbsolutePath));
            }
            ToastUtils.showToast(appContext, "视频文件保存在" + mNextVideoAbsolutePath);
        }
    }

    /**
     * 暂停录制
     */
    public void pauseRecordingVideo() {
        stopRecordingVideo(false);
    }

    /**
     * 开始视频录制
     */
    private void startRecordingVideo() {

        Log.i(TAG, " startRecordingVideo  录制初始化 ");
        TextureView mTextureView = getTextureView();
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }

        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // 为相机预览设置Surface
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // 为 MediaRecorder设置Surface
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    Log.i(TAG, " startRecordingVideo  正式开始录制 ");
                    getTextureViewContext().runOnUiThread(() -> {
                        mIsRecordingVideo = true;
                        isRecordGonging =true;
                        mMediaRecorder.start();
                        if (camera2VideoRecordCallBack != null) {
                            camera2VideoRecordCallBack.startRecord();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getTextureViewContext();
                    if (null != activity) {
                        Toast.makeText(activity.getApplicationContext(), "相机设备配置失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 设置媒体录制器的配置参数
     * <p>
     * 音频，视频格式，文件路径，频率，编码格式等等
     *
     * @throws IOException
     */
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getTextureViewContext();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mNextVideoAbsolutePath = FileUtils.createVideoDiskFile(appContext, FileUtils.createVideoFileName()).getAbsolutePath();
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        //每秒30帧
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(ORIENTATIONS.get(rotation));
                break;
            default:
                break;
        }
        mMediaRecorder.prepare();
    }

    private List<String> oldVideoPath;

    @Override
    public void startOperate() {
        TextureView textureView = getTextureView();
        if (textureView.isAvailable()) {
            openCamera(getTextureViewContext(), textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void stopOperate() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }
    private  boolean isRecordGonging =false;

    /**
     * 是否在进行视频录制，录制状态，包含进行中，暂停中。
     * @return
     */
    public  boolean isVideoRecord(){
        return isRecordGonging;
    }

    @Override
    protected void openCamera(Activity activity, int width, int height) {
        if (PermissionsManager.checkVideoRecordPermission(getTextureViewContext())) {
            if (null == activity || activity.isFinishing()) {
                return;
            }
            Log.i(TAG, "视频录制，重新配置相机设备");
            AutoFitTextureView textureView = (AutoFitTextureView) getTextureView();
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                Log.d(TAG, "tryAcquire");
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("锁住相机开启，超时");
                }
                //通常情况下，后摄像头是0，前摄像头是1
                String cameraId = manager.getCameraIdList()[currentDirection==CameraCharacteristics.LENS_FACING_BACK?0:1];
                // 计算相机预览和视频录制的的Size
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (map == null) {
                    throw new RuntimeException("Cannot get available preview/video sizes");
                }
                mVideoSize = Camera2CalculationUtils.chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                mPreviewSize = Camera2CalculationUtils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, mVideoSize);

                int orientation = activity.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    textureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                configureTransform(activity, width, height);
                mMediaRecorder = new MediaRecorder();
                manager.openCamera(cameraId, stateCallback, null);
            } catch (CameraAccessException e) {
                ToastUtils.showToast(appContext, "不能访问相机");
                activity.finish();
            } catch (NullPointerException e) {
                ToastUtils.showToast(appContext, "当前设备不支持Camera2 API");
            } catch (InterruptedException e) {
                throw new RuntimeException("在锁住相机开启期间被打断.");
            }
        }
    }

    @Override
    protected void configureTransform(Activity activity, int viewWidth, int viewHeight) {
        TextureView textureView = getTextureView();
        if (null == textureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private Camera2VideoRecordCallBack camera2VideoRecordCallBack;

    public void setCamera2VideoRecordCallBack(Camera2VideoRecordCallBack camera2VideoRecordCallBack) {
        this.camera2VideoRecordCallBack = camera2VideoRecordCallBack;
    }
}
