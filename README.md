# Camera2App

采用android5.0中Camera2 api+MVP模式开发的一个相机程序。

运行效果如下：

![image](https://github.com/13767004362/Camera2App/blob/master/app/device-2018-02-06-144135.png)



-----
包含以下功能：

- 拍照

- 录像：单次录像，暂停与恢复录像。

- 模式设置：自动调焦，闪关灯，手动调焦, zoom调焦


----

采用的技术点：

- Camera2 API：相机API

- soviewer.jar: 用于将多个视频合成一个视频

- RxJava和RxAndroid: 异步线程和主线程通讯

- Glide : 异步加载图片和视频

----


备注：
虽然andorid 5.0 出现了camera 2 API特性，但各大android手机系统厂商对camera 2 的支持力度不一致。
因此，实际开发中还应该通过判断运行手机对camera2 支持力度来决定是否需要用回camera来开发相机。

**Android Camera2 API资料阅览**
---

**简介**：
>Android 5.0开始出现了新的相机Camera 2 API，用来替代以前的camera api。
>
>Camera2 API不仅提高了android系统的拍照性能，还支持RAW照片输出，还可以设置相机的对焦模式，曝光模式，快门等等。


**Camera2 中主要的API**:

**类**：

- **CameraManager类** : 摄像头管理类，用于检测、打开系统摄像头，通过`getCameraCharacteristics(cameraId)`可以获取摄像头特征。

- **CameraCharacteristics类**：相机特性类，例如，是否支持自动调焦，是否支持zoom，是否支持闪光灯一系列特征。

- **CameraDevice类**： 相机设备,类似早期的camera类。

- **CameraCaptureSession类**：用于创建预览、拍照的Session类。通过它的`setRepeatingRequest()`方法控制预览界面 , 通过它的`capture()`方法控制拍照动作或者录像动作。

- **CameraRequest类**：一次捕获的请求，可以设置一些列的参数，用于控制预览和拍照参数，例如：对焦模式，曝光模式，zoom参数等等。  


接下来，进一步介绍，Camera2 API中的各种常见类和抽象类。

**CameraManager类**
---

```
  CameraCharacteristics cameraCharacteristics =manager.getCameraCharacteristics(cameraId);
```
通过以上代码可以获取摄像头的特征对象，例如： 前后摄像头，分辨率等。

**CameraCharacteristics类**
---
相机特性类


CameraCharacteristics是一个包含相机参数的对象，可以通过一些key获取对应的values.

**以下几种常用的参数**：

- LENS_FACING:获取摄像头方向。LENS_FACING_FRONT是前摄像头，LENS_FACING_BACK是后摄像头。

- SENSOR_ORIENTATION：获取摄像头拍照的方向。

- FLASH_INFO_AVAILABLE：获取是否支持闪光灯。

- SCALER_AVAILABLE_MAX_DIGITAL_ZOOM：获取最大的数字调焦值，也就是zoom最大值。 

- LENS_INFO_MINIMUM_FOCUS_DISTANCE：获取最小的调焦距离，某些手机上获取到的该values为null或者0.0。前摄像头大部分有固定焦距，无法调节。

- INFO_SUPPORTED_HARDWARE_LEVEL：获取摄像头支持某些特性的程度。 
  
   以下手机中支持的若干种程度：

   - INFO_SUPPORTED_HARDWARE_LEVEL_FULL：全方位的硬件支持，允许手动控制全高清的摄像、支持连拍模式以及其他新特性。
   
   - INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED：有限支持，这个需要单独查询。
   
   - INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY：所有设备都会支持，也就是和过时的Camera API支持的特性是一致的。


**CameraDevice类**
---

CameraDevice的`reateCaptureRequest(int templateType)`方法创建CaptureRequest.Builder。

templateType参数有以下几种：

- TEMPLATE_PREVIEW ：预览

- TEMPLATE_RECORD：拍摄视频

- TEMPLATE_STILL_CAPTURE：拍照

- TEMPLATE_VIDEO_SNAPSHOT：创建视视频录制时截屏的请求

- TEMPLATE_ZERO_SHUTTER_LAG：创建一个适用于零快门延迟的请求。在不影响预览帧率的情况下最大化图像质量。

- TEMPLATE_MANUAL：创建一个基本捕获请求，这种请求中所有的自动控制都是禁用的(自动曝光，自动白平衡、自动焦点)。


**CameraDevice.StateCallback抽象类**
---

该抽象类用于CemeraDevice相机设备状态的回调。
```
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

            mCameraDevice = cameraDevice;
            startPreView();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        
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
            cameraDevice.close();
            mCameraDevice = null;
         
        }
        /**
         *当相机被关闭的时候
         */
        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
        }
    };
```

**CameraCaptureSession.StateCallback抽象类**
---
该抽象类用于Session过程中状态的回调。
```
public static abstract class StateCallback {

        //摄像头完成配置，可以处理Capture请求了。
        public abstract void onConfigured(@NonNull CameraCaptureSession session);
        
        //摄像头配置失败
        public abstract void onConfigureFailed(@NonNull CameraCaptureSession session);
        
        //摄像头处于就绪状态，当前没有请求需要处理
        public void onReady(@NonNull CameraCaptureSession session) {}
        
        //摄像头正在处理请求
        public void onActive(@NonNull CameraCaptureSession session) {}
        
        //请求队列中为空，准备着接受下一个请求。
        public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {}
        
        //会话被关闭
        public void onClosed(@NonNull CameraCaptureSession session) {}
        
        //Surface准备就绪
        public void onSurfacePrepared(@NonNull CameraCaptureSession session,@NonNull Surface surface) {}
        
}
```

**资源参考**：

- **官方关于camera2 博客介绍**:https://medium.com/google-developers/detecting-camera-features-with-camera2-61675bb7d1bf

- **极客学院关于Camera2的介绍**： http://wiki.jikexueyuan.com/project/android-actual-combat-skills/android-hardware-camera2-operating-guide.html

- [**Google的CameraView库**](https://github.com/google/cameraview)

- [android-Camera2Basic](https://github.com/googlesamples/android-Camera2Basic/#readme)

- [android-Camera2Raw](https://github.com/googlesamples/android-Camera2Raw/#readme)

- [官方的android-Camera2Video](https://github.com/googlesamples/android-Camera2Video/#readme)


