# CameraXApp

一个 CameraX + OpenGL + MediaCodec + MediaMuxer 实现的相机App。

***当前相机app支持功能***：

- 基础功能:
    - 前后摄像头切换(support
    - 聚焦:自动聚焦、点击聚焦(support)
    - 数字变焦：手势缩放、滑块缩放(support)
    - 闪光灯模式切换(自动、常亮、关闭)(support)
    - 图片拍照(support)
    - 视频录制mp4,包含音频(support)

- 美颜功能(feature,下期开发中)
    - 美白
    - 瘦脸
    - 大眼
    - 贴纸
    - 滤镜

运行效果：
![image](https://github.com/13767004362/Camera2App/blob/master/app/device-2018-02-06-144135.png)

***app 技术架构**：

```
┌─────────────────────────────────────────────────────────────────┐
│                    相机+音视频录制流程架构                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────────┐   │
│  │   CameraX   │───▶│  OpenGL纹理   │───▶│ 编码Surface渲染 │   │
│  │  相机预览    │    │   处理       │    │ (MediaCodec输入)│   │
│  └─────────────┘    └──────────────┘    └─────────────────┘   │
│         │                                        │             │
│         │                                        ▼             │
│  ┌─────────────┐                              ┌─────────────────┐   │
│  │  预览Surface  │                              │   视频编码器     │   │
│  │  (用户可见)  │                              │   (H.264)      │   │
│  └─────────────┘                              └─────────────────┘   │
│                                                      │             │
│  ┌─────────────┐    ┌──────────────┐              │             │
│  │ AudioRecord │───▶│  音频编码器   │─────────────┤             │
│  │   音频采集    │    │   (AAC)      │              ▼             │
│  └─────────────┘    └──────────────┘    ┌─────────────────┐   │
│                                            │   MediaMuxer    │   │
│                                            │   合成MP4文件    │   │
│                                            └─────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

架构角色

- camerax: 相机采集
- opengl: 图像绘制
- audioRecord: 音频采集
- mediacodec: 音视频编码
- muxer: 音视频合成mp4

#### Camerax相关的说明

**Camerax中UseCase**
CameraX 提供的 supportedSizes 列表中的 Size (宽x高) 几乎总是基于传感器的自然方向（通常是横向，例如
4000x3000, 1920x1080）。
与手机屏幕方向（通常是竖向，例如 1080x1920, 3000x4000）相反的。

- Preview：用于在屏幕上显示相机预览，尺寸受SurfaceView/TextureView大小影响，可以设置分辨率大小。
- ImageCapture：用于拍摄高质量照片, 可以设置分辨率大小。
- ImageAnalysis：用于实时图像处理和分析，可以设置分辨率大小。
- VideoCapture：用于录制视频，不直接设置分辨率，通过QualitySelector控制质量（SD、HD、FHD、UHD）。

**Camerax的支持尺寸列表**
选择一个传感器方向的supportedSizes 的 Size，camerax 会自动旋转、裁剪、填充以适应屏幕。

ResolutionSelector：用于 ImageCapture、Preview 和
ImageAnalysis。它允许你通过宽高比（AspectRatioStrategy）和具体尺寸（ResolutionStrategy）来非常精确地控制分辨率（例如
1920x1080）。这对于静态图像分析或需要精确裁剪的预览非常重要。

ResolutionSelector的函数：

- setAspectRatioStrategy：专门处理宽高比匹配（它会正确处理旋转问题）。
- setResolutionStrategy：当有多个分辨率符合宽高比时，用它来选择一个（例如选择最高的、或最接近
  targetWidth x targetHeight 的）。
- setResolutionFilter：应该只用于最后的精细过滤（例如，“去掉所有小于 100万 像素的”）。

**Camerax中旋转角度**
rotationDegrees:

- 0度:竖屏;
- 90度:顺时针旋转90度,横屏;
- 180度顺时针旋转180度,上下颠倒的竖屏;
- 270度:顺时针270度或者逆时针90度,横屏

**Camerax中视频录制**

QualitySelector：专门用于 VideoCapture。视频录制不仅仅是分辨率，它还涉及编码器配置、帧率和比特率。QualitySelector
将这些复杂的配置抽象为简单的“质量”级别（如 UHD、FHD、HD、SD），这与 Android 设备的
CamcorderProfile（摄像机配置文件）紧密相关。

视频分辨率:

- SD(标清): 720×480 (NTSC) 或 720×576 (PAL)，最低的视频质量标准,文件较小,显示效果较差
- HD(高清): 1280×720 (720p),文件大小适中,目前网络视频的常见标准
- FHD(全高清):1920×1080 (1080p),目前大多数高清电视和显示器的标准,提供高质量的视觉体验
- UHD(超高清): 3840×2160 (2160p/4K),文件很大,提供极致的视觉体验，下一代显示技术的标准

#### MediaCodec相关的说明

**MediaCodec编码模式**

- ByteBuffer 模式：
    - 格式：COLOR_FORMAT 对应的值是 MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar(
      图像格式 NV21)。
    - 操作：通过 MediaCodec.dequeueInputBuffer() 获取数据输入缓冲区，再通过
      MediaCodec.queueInputBuffer() 手动将 YUV 图像传给 MediaCodec。
    - 结束标识：queueInputBuffer(..., BUFFER_FLAG_END_OF_STREAM)
- Surface 模式（推荐使用）:
    - 格式：COLOR_FORMAT 对应的值是 MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface。
    - 操作：通过 MediaCodec.createInputSurface() 创建编码数据源 Surface，再通过 OpenGL 纹理，将相机预览图像绘制到该
      Surface 上。
    - 结束标识：MediaCodec.signalEndOfInputStream()

camerax + mediacodec 视频编码的实现思路：

```
Camera → OES Texture → FBO(美颜处理)
           ↓
        draw to FBO texture
           ↓
        draw to preview EGLSurface
           ↓
        draw to encoder EGLSurface → eglSwapBuffers() → 编码帧产生
```

##### 音视频编码同轨pts计算

**视频帧的pts 计算**
mediacodec 的surface模式,进行视频编码时,计算pts 的方式有3种：

```kotlin
// 1. 基于帧率的理论时间戳
val frameTimeNs = frameIndex * 1000000000L / frameRate

// 2. 基于系统时间的实时时间戳  
val elapsedTimeNs = (System.nanoTime() - startTimeNs)

// 3. 相机硬件时间戳（推荐）
val cameraTimestampNs = surfaceTexture.getTimestamp() 
```

接着通过`EGLExt.eglPresentationTimeANDROID() `设置正确的时间戳。mediacodec 编码出来的数据包,单位是微妙。

***音频帧的pts***
mediaCodec的ByteBuffer模式，进行音频编码时,pts 的计算方式有2种：

```kotlin
// 1. 基于帧率的理论时间戳
val bytesPerSample = 2 // 16-bit = 2字节
val channel = 1
val sampleRate = 44100
val samplesPerChannel = size / (channel * bytesPerSample)
// 计算持续时间,单位微妙
val durationUs = (1000000 * samplesPerChannel / sampleRate).toLong()

// 2. 基于系统时间的实时时间戳  
val durationUs = (System.nanoTime() - startTimeNs) / 1000
```

接着通过`mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, size, durationUs, 0)`设置正确时间戳。mediacodec
编码出来的数据包,单位是微妙。

虽然 视频流中EGL输入纳秒，音频直接输入微秒，但MediaCodec输出统一为微秒。