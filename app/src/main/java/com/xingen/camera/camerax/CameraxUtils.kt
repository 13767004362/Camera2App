package com.xingen.camera.camerax

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaRecorder
import android.util.Log
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import java.lang.Float
import kotlin.Int
import kotlin.let
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by HeXinGen  on 2025/11/11
 * Description:.
 */
object CameraxUtils {


    /**
     * 打印相机支持的预览、录频、照片(yuv、jpeg)
     */
    @SuppressWarnings("all")
    fun printCameraInfo(cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector) {
        // 查到到对应相机的尺寸
        val cameraInfo = cameraProvider.availableCameraInfos.firstOrNull { info ->
            cameraSelector.filter(listOf(info)).isNotEmpty()
        }
        cameraInfo?.let {
            // 获取相机特性
            val cameraCharacteristics = Camera2CameraInfo.extractCameraCharacteristics(it)

            // 获取StreamConfigurationMap
            val map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            // 获取支持的预览尺寸
            val previewSizes = map?.getOutputSizes(SurfaceTexture::class.java)
            previewSizes?.forEach { size ->
                Log.d("CameraX", "Preview size: ${size.width}x${size.height}")
            }
            if (previewSizes != null) {
                val optimalSize = chooseOptimalSize(previewSizes.toList(), 1280, 720)
                Log.d("CameraX", "Optimal preview size: ${optimalSize.width}x${optimalSize.height}")
            }


            //视频录制尺寸
            val mediaRecorder = map?.getOutputSizes(MediaRecorder::class.java)
            mediaRecorder?.forEach { size ->
                Log.d("CameraX", "MediaRecorder size: ${size.width}x${size.height}")
            }

            // 获取支持的 JPEG 尺寸
            val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG)
            jpegSizes?.forEach { size ->
                Log.d("CameraX", "JPEG Size: ${size.width} x ${size.height}")
            }

            // 获取支持照片的 YUV 尺寸
            val yuvSizes = map?.getOutputSizes(ImageFormat.YUV_420_888)
            yuvSizes?.forEach { size ->
                Log.d("CameraX", "YUV Size: ${size.width} x ${size.height}")
            }
        }
    }

    /**
     * 选择目标宽高接近的相机尺寸
     */
    fun createResolutionSelector(
        targetWidth: Int,
        targetHeight: Int
    ): ResolutionSelector {

        // 1. 定义目标尺寸
        val targetResolution = Size(targetWidth, targetHeight)
        // 2. 定义目标宽高比
        val aspectRatioStrategy = AspectRatioStrategy(
            AspectRatio.RATIO_16_9, // 传入精确的目标比例
            AspectRatioStrategy.FALLBACK_RULE_AUTO // 降级策略
        )

        // 3. 定义分辨率选择策略
        // 这个策略会选择所有符合宽高比的尺寸中，面积 >= 目标面积的尺寸
        // 如果没有，它会选择最接近目标面积的尺寸
        val resolutionStrategy = ResolutionStrategy(
            targetResolution, // 传入目标尺寸
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER // 降级策略
        )

        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatioStrategy) // 1. 先匹配宽高比
            .setResolutionStrategy(resolutionStrategy)  // 2. 再从匹配的当中选一个最好的
            .build()
    }
    /**
     * 选择目标宽高接近的相机尺寸，自定义实现筛选逻辑
     */
    fun createCustomResolutionSelector(
        targetWidth: Int,
        targetHeight: Int
    ): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setResolutionFilter(object : ResolutionFilter {
                override fun filter(
                    supportedSizes: List<Size>,
                    rotationDegrees: Int
                ): List<Size> {
                    //相机预览尺寸中宽高,是基于传感器自然方向（通常是横向，例如 4000x3000, 1920x1080）
                    //与手机屏幕方向（通常是竖向，例如 1080x1920, 3000x4000）相反的。
                    val width: Int = max(targetWidth, targetHeight)
                    val height: Int = min(targetWidth, targetHeight)
                    val filterList = arrayListOf<Size>()
                    // 首先查找目标尺寸等宽等高的
                    for (size in supportedSizes) {
                        if (size.width == width && size.height == height) {
                            filterList.add(size)
                            return@filter filterList
                        }
                    }
                    val targetRatio = width * 1f / height
                    // 其次选择宽高比率一致的尺寸
                    for (size in supportedSizes) {
                        if ((size.width * 1f / size.height == targetRatio)&&size.width>=targetWidth) {
                            filterList.add(size)
                        }
                    }
                    if (filterList.isNotEmpty()) {
                        return filterList
                    }
                    // 最后选择比例最相近的
                    var minDiff = Float.MAX_VALUE
                    var optimalSize: Size? = null
                    for (option in supportedSizes) {
                        val ratio = option.width.toFloat() / option.height
                        val diff = abs(ratio - targetRatio)
                        if (diff < minDiff) {
                            optimalSize = option
                            minDiff = diff
                        }
                    }
                    filterList.add(optimalSize ?: supportedSizes[0])
                    return filterList
                }
            })
            .build()
    }

    /**
     * 选择视频分辨率
     */
    fun  createQualitySelector(): QualitySelector{
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(
                Quality.FHD, // 尝试 1080p
                Quality.HD,  // 其次尝试 720p
                Quality.SD   // 最后尝试 480p
            ),
            // 如果列表中的都不支持，就选择一个比 SD 质量更低或更高的最接近的质量
            // 这是为了确保总能找到一个可用的配置
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )
        return qualitySelector
    }


    /**
     * 选择相机中尺寸列表中与目标尺寸最接近的尺寸
     */
    fun chooseOptimalSize(
        choices: List<Size>,
        targetWidth: Int,
        targetHeight: Int
    ): Size {
        val screenRatio = targetWidth.toFloat() / targetHeight

        var optimalSize: Size? = null
        var minDiff = Float.MAX_VALUE

        for (option in choices) {
            val ratio = option.width.toFloat() / option.height
            val diff = abs(ratio - screenRatio)
            if (diff < minDiff) {
                optimalSize = option
                minDiff = diff
            }
        }
        return optimalSize ?: choices[0]
    }
}