package com.xingen.camera.camera2.operate;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.xingen.camera.camera2.BaseCamera2Operator;
import com.xingen.camera.camera2.Camera2Manager;
import com.xingen.camera.camera2.utils.Camera2Utils;
import com.xingen.camera.camera2.compare.CompareSizeByArea;
import com.xingen.camera.utils.permissions.PermissionsManager;
import com.xingen.camera.utils.rxjava.ObservableBuilder;
import com.xingen.camera.utils.thread.WorkThreadUtils;
import com.xingen.camera.utils.toast.ToastUtils;
import com.xingen.camera.widget.AutoFitTextureView;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Created by ${xinGen} on 2017/10/21.
 * <p>
 * 拍照操作类
 */

public class PictureOperater extends BaseCamera2Operator {
    private static final String TAG = PictureOperater.class.getSimpleName();
    private WorkThreadUtils workThreadManager;
    /**
     * 相机最大的预览宽度
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;
    /**
     * 相机最大的预览高度
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    /**
     * 处理静态图片的输出
     */
    private ImageReader imageReader;
    /**
     * 相机传感器方向
     */
    private int mSensorOrientation;
    /**
     * 相机预览的大小Size
     */
    private Size mPreviewSize;
    /**
     * 是否支持自动对焦
     */
    private boolean mAutoFocusSupported;
    /**
     * 是否支持闪光灯
     */
    private boolean mFlashSupported;
    /**
     * 相机的Id
     */
    private String mCameraId;

    /**
     * 预览请求的Builder
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;
    /**
     * 预览的请求
     */
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession mCaptureSession;
    /**
     * 开启相机的锁住时间
     */
    private final int LOCK_TIME = 2500;

    private Camera2Manager camera2Manager;

    /**
     * 最小的焦距
     */
    private float minimalFocalDistance = 0;
    /**
     * 最大的数字变焦值，缩放值
     */
    private float maxZoom = 0;


    public PictureOperater(Camera2Manager camera2Manager) {
        this.camera2Manager = camera2Manager;
        this.workThreadManager = camera2Manager.getWorkThreadManager();
    }

    @Override
    public void startOperate() {
        //  Log.i(TAG, " 打开相机的操作  startOperate ");
        TextureView textureView = getTextureView();
        /**
         *当打开锁屏后，TextureView是可用的。
         *
         * 可用时候，开启相机，且出现预览界面。
         * 反之，不可用时候，进入SurfaceTextureListener中，等待到TextureView可用为止。
         */
        if (textureView.isAvailable()) {
            openCamera(getTextureViewContext(), textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void stopOperate() {
        //   Log.i(TAG, " 关闭相机的操作 ");
        closeCamera();
    }

    @Override
    public void openCamera(Activity activity, int width, int height) {
        if (PermissionsManager.checkCameraPermission(activity)) {
            setUpCameraOutputs(activity, width, height);
            configureTransform(activity, width, height);
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                //打开相机需要一定时间，因锁住2.5秒，防止程序退出关闭相机
                if (!mCameraOpenCloseLock.tryAcquire(LOCK_TIME, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                manager.openCamera(mCameraId, stateCallback, workThreadManager.getBackgroundHandler());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        }
    }

    @Override
    public void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    @Override
    public void configureTransform(Activity activity, int viewWidth, int viewHeight) {
        if (null == getTextureView() || null == mPreviewSize || null == activity) {
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
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        getTextureView().setTransform(matrix);
    }

    @Override
    public void writePictureData(Image image) {
        if (camera2ResultCallBack != null) {
            camera2ResultCallBack.callBack(ObservableBuilder.createWriteCaptureImage(appContext, image));
        }
    }

    @Override
    public void startPreView() {
        //开启相机预览界面
        createCameraPreviewSession();
    }

    @Override
    public void cameraClick() {
        takePicture();
    }

    /**
     * 关闭当前的相机设备，释放资源
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closi" +
                    "ng.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * 拍照
     */
    public void takePicture() {
        if (mAutoFocusSupported) {
            lockFocus();
        } else {//设备不支持自动对焦，则直接拍照。
            captureStillPicture();
        }
    }

    /**
     * 拍照的第一步，锁住焦点。
     */
    private void lockFocus() {
        try {
            //告诉相机，这里已经锁住焦点
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // 标识，正在进行拍照动作
            mState = STATE_WAITING_LOCK;
            //进行拍照处理
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 相机开始预览，创建一个CameraCaptureSession对象
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = getTextureView().getSurfaceTexture();
            //assert断言表达式，为true继续。
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // 创建一个预览界面需要用到的Surface对象
            Surface surface = new Surface(texture);

            // 将CaptureRequest的构建器与Surface对象绑定在一起
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // 为相机预览，创建一个CameraCaptureSession对象
            mCameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // 相机已经关闭
                    if (null == mCameraDevice) {
                        return;
                    }
                    //当cameraCaptureSession已经准备完成，开始显示预览界面
                    mCaptureSession = cameraCaptureSession;
                    setCameraCaptureSession();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //配置失败
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 设置CameraCaptureSession的特征:
     * <p>
     * 自动对焦，闪光灯
     */
    private void setCameraCaptureSession() {
        try {
            setFocus(mPreviewRequestBuilder);
            //若是需要则开启，闪光灯
            setAutoFlash(mPreviewRequestBuilder);
            // 最后，开启相机预览界面的显示
            mPreviewRequest = mPreviewRequestBuilder.build();

            //为CameraCaptureSession设置复用的CaptureRequest。
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private float currentZoom;

    @Override
    public void notifyFocusState() {
        if (mPreviewRequestBuilder != null) {
            try {
                currentZoom = maxZoom * camera2Manager.getZoomProportion();
                setZoom(currentZoom);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Rect zoomRect;

    private void setZoom(float currentZoom) {
        try {
          //  mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, currentZoom);
            zoomRect=createZoomReact();
            if (zoomRect==null){
                Log.i(TAG, "相机不支持 zoom " );
                return ;
            }
            mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION,zoomRect);
            mPreviewRequest = mPreviewRequestBuilder.build();
            Log.i(TAG, " 最大缩放值 " + maxZoom + " 设置缩放值 " + currentZoom );
            //为CameraCaptureSession设置复用的CaptureRequest。
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, workThreadManager.getBackgroundHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算出zoom所对应的裁剪区域
     * @return
     */
    private Rect createZoomReact() {
        if (currentZoom==0){
            return null;
        }
        try {
            Rect rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (rect==null){
                return null;
            }
            zoomRect =Camera2Utils.createZoomRect(rect,currentZoom);
            Log.i(TAG, "zoom对应的 rect对应的区域 " + zoomRect.left + " " + zoomRect.right + " " + zoomRect.top + " " + zoomRect.bottom);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zoomRect;
    }

    /**
     * -
     * 设置调焦方式：自动连续对焦还是手动调焦
     *
     * @param requestBuilder
     */
    private void setFocus(CaptureRequest.Builder requestBuilder) {
        if (camera2Manager.isManualFocus()) {
            float focusDistance = minimum_focus_distance * camera2Manager.getZoomProportion();
            setManualFocus(requestBuilder, focusDistance);
        } else {
            setAutoFocus(requestBuilder);
        }
    }

    /**
     * 设置连续自动对焦
     *
     * @param requestBuilder
     */
    private void setAutoFocus(CaptureRequest.Builder requestBuilder) {
        if (requestBuilder != null) {
            //为相机预览设置连续对焦。
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        }
    }

    /**
     * 设置手动对焦，设置焦距值
     *
     * @param requestBuilder
     */
    private void setManualFocus(CaptureRequest.Builder requestBuilder, float distance) {
        //若是机器不支持焦距设置，则需要检查。
        try {
            if (requestBuilder != null) {
                //先关闭自动对焦的模式
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                Log.i(TAG, "手动调焦的 " + distance + " 最大范围值是 " + minimum_focus_distance);
                //设置焦距值
                requestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, distance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Camera state: Showing camera preview.
     * 相机预览状态
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     * <p>
     * 相机拍照，被锁住，等待焦点状态
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     * 图片已经获取
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;
    /**
     * CameraCaptureSession.CaptureCallback : 处理捕获到的JPEG事件。
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            switch (mState) {
                //正常预览状态
                case STATE_PREVIEW: {
                    break;
                }
                //刚开始拍照，锁住，等待状态
                case STATE_WAITING_LOCK: {
                    //当前自动对焦的状态
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                //等待，预捕获
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                //已经完成预捕获，直接拍照。
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;

                }
                default:
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }
    };

    /**
     * <p>
     * 运行预捕获的序列，为捕获一个静态图片
     */
    private void runPrecaptureSequence() {
        try {
            // 告诉相机，这里触发.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            //设置成预捕获状态，将需等待。
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照一个静态的图片
     * ,当在CaptureCallback监听器响应的时候调用该方法。
     * <p>
     * 当数字调焦缩放的时候，在写入图片数中也要设置。
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getTextureViewContext();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // 创建一个拍照的CaptureRequest.Builder
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(imageReader.getSurface());
            // 使用相同的AE和AF模式作为预览.
             captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            //让相机中缩放效果和图片保持一致
            //   zoomRect=createZoomReact();
            if (zoomRect != null) {
                Log.i(TAG," 拍照 添加裁剪区域 "+zoomRect.toString());
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }
            setAutoFlash(captureBuilder);
            // 方向
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, Camera2Utils.getOrientation(ORIENTATIONS, mSensorOrientation, rotation));
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session
                        , @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    //拍照完成，进行释放焦点操作。
                    unlockFocus();
                }
            };
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 完成一些列拍照后，释放焦点。
     */
    private void unlockFocus() {
        try {
            // 重置一系列的对焦
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, workThreadManager.getBackgroundHandler());
            // 恢复正常状态
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置是否开启闪光灯
     *
     * @param requestBuilder
     */
    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * 获取到最大的焦距值
     */
    private float minimum_focus_distance;
    /**
     * 获取相机参数的类
     */
    private CameraCharacteristics cameraCharacteristics;

    /**
     * 设置Camera的相关参数变量，长、宽，且返回相机的Id.
     *
     * @param width
     * @param height
     */
    private void setUpCameraOutputs(Activity activity, int width, int height) {
        //  Log.i(TAG," 检查是否支持camera2 "+ Camera2Utils.hasCamera2(getTextureViewContext()));
        CameraManager manager = (CameraManager) getTextureViewContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            //获取到可用的相机
            for (String cameraId : manager.getCameraIdList()) {
                //获取到每个相机的参数对象，包含前后摄像头，分辨率等
                cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                if (!Camera2Utils.matchCameraDirection(cameraCharacteristics, currentDirection)) {
                    continue;
                }
                //存储流配置类
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                //检查设备,是否支持自动对焦
                mAutoFocusSupported = Camera2Utils.checkAutoFocus(cameraCharacteristics);
                //获取最小焦距值。
                Float minFocalDistance = Camera2Utils.getMinimumFocusDistance(cameraCharacteristics);
                if (minFocalDistance != null) {
                    minimum_focus_distance = minFocalDistance;
                }

                Float maxZoomValue = Camera2Utils.getMaxZoom(cameraCharacteristics);
                if (maxZoomValue != null) {
                    maxZoom = maxZoomValue;
                }
                Log.i(TAG, (currentDirection == CameraCharacteristics.LENS_FACING_BACK ? "后" : "前") + " 摄像头 " + " 是否支持自动对焦 " + mAutoFocusSupported + " 获取到焦距的最大值 " + minimum_focus_distance + " 最大的缩放值 " + maxZoom);
                //对于静态图片，使用可用的最大值来拍摄。
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizeByArea());
                //设置ImageReader,将大小，图片格式
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, workThreadManager.getBackgroundHandler());
                //获取到屏幕的旋转角度，进一步判断是否，需要交换维度来获取预览大小
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //获取相机传感器方向
                mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }
                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                //当角度反了的时候
                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }
                mPreviewSize = Camera2Utils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest, new CompareSizeByArea());
                // 计算出来的预览大小，设置成TextureView宽高.
                int orientation = activity.getResources().getConfiguration().orientation;
                AutoFitTextureView mTextureView = (AutoFitTextureView) getTextureView();
                //横屏
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                // 检查，相机是否支持闪光。
                Boolean available = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                mCameraId = cameraId;
                // Log.i(TAG, " 根据相机的前后摄像头" + mCameraId + " 方向是：" + currentDirection);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            //不支持该设备
            if (e instanceof NullPointerException) {
                ToastUtils.showToast(appContext, "设备不支持Camera2 API");
            }
        }
    }


}
