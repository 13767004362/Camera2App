package com.xingen.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.xingen.camera.base.BaseVMActivity
import com.xingen.camera.camerax.CameraxUtils
import com.xingen.camera.camerax.VideoEncodeConfig
import com.xingen.camera.databinding.ActivityMainBinding
import com.xingen.camera.opengl.GLThread
import com.xingen.camera.view.systembar.SystemBarUtils.setStickyStyle
import com.xingen.camera.view.widget.VerticalProgressBarLayout
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Created by ${xinGen} on 2017/10/19.
 */
class MainActivity : BaseVMActivity<ActivityMainBinding, MainViewModel>() {
    var glThread: GLThread? = null

    // 默认后摄像头
    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraControl: CameraControl
    private lateinit var cameraInfo: CameraInfo
    override fun initViewImpl(savedInstanceState: Bundle?) {
        Log.i(TAG, "initViewImpl")
        binding.cameraRightTopController.setOnClickListener {
            if (viewModel.isRecording()) {
                // 停止录像
                glThread?.setEncode(false)
                viewModel.stopRecord()
            } else {
                // 开始录像
                viewModel.recordVideo()
                glThread?.setEncode(true)
            }
        }
        binding.cameraBtn.setOnClickListener {
            glThread?.takePhoto()
        }

        binding.cameraDirectionRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.camera_direction_front) {
                // 切换到前摄像头
                if (cameraSelector != CameraSelector.DEFAULT_FRONT_CAMERA) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    startCamera()
                }
            } else {
                // 切换到后摄像头
                if (cameraSelector != CameraSelector.DEFAULT_BACK_CAMERA) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    startCamera()
                }
            }
        }
        binding.cameraShow.setOnClickListener {
            currentImgUri?.run {
                PictureActivity.openActivity(this@MainActivity, this)
            }
        }
        requestCameraPermission()
        initGLThread()
    }

    private fun requestCameraPermission() {
        if (allPermissionsGranted()) {
            // 如果已经有权限，则开启相机
            waitSurfaceViewReady()
        } else {
            // 如果没有权限，则请求权限
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private var timerSeconds = 0
    private val timerTask = object : Runnable {
        override fun run() {
            timerSeconds++
            // 格式化时间并更新 UI
            val minutes = timerSeconds / 60
            val seconds = timerSeconds % 60
            binding.cameraVideoRecordTipTimeTv.text =
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            // 每秒执行一次
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //焦点改变的时候 设置隐藏系统状态栏
        setStickyStyle(window)
    }


    private var imageSurfaceTexture: SurfaceTexture? = null
    private fun initGLThread() {
        Log.i(TAG, "initGLThread")
        glThread = GLThread()
        val rotation = if (Build.VERSION.SDK_INT >= 30) {
            display.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
        Log.i(TAG, "activity rotation:$rotation")
        glThread?.setRotation(rotation)
        glThread?.onFrameAvailableListener = object : GLThread.OnFrameAvailableListener {
            override fun onFrameAvailable(imageSurfaceTexture: SurfaceTexture) {
                this@MainActivity.imageSurfaceTexture = imageSurfaceTexture
                // 等待纹理创建成后，设置到camerax 中preview中
                startCamera()
            }
        }
        glThread?.screenshotListener = object : GLThread.ScreenshotListener {
            override fun onScreenshotCaptured(bitmap: Bitmap) {
                viewModel.handleBitmapToGallery(bitmap)
            }
        }
    }

    override fun subViewModel() {
        viewModel.isRecordingState.observe(this) {
            if (it) {
                binding.cameraVideoRecordTipTimeTv.visibility = View.VISIBLE
                binding.cameraVideoRecordTipBg.visibility = View.VISIBLE
                binding.cameraRightTopController.setImageResource(R.drawable.camera_stop_iv)
                timerSeconds = 0
                mainHandler.postDelayed(timerTask, 1000)
            } else {
                binding.cameraVideoRecordTipTimeTv.let { i ->
                    i.text = ""
                    i.visibility = View.GONE
                }
                binding.cameraVideoRecordTipBg.visibility = View.GONE
                binding.cameraRightTopController.setImageResource(R.drawable.camera_init_iv)
                mainHandler.removeCallbacks(timerTask)
            }
        }
        viewModel.takePictureEvent.observe(this) {
            if (it.success) {
                val msg = "拍照成功"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
                currentImgUri = it.uri
                Glide.with(this@MainActivity).load(it.uri!!).into(binding.cameraShow)
            } else {
                val msg = "拍照失败: ${it.error!!.message}"
                Log.e(TAG, msg, it.error)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.recordVideoEvent.observe(this) {
            if (it) {
                Toast.makeText(baseContext, "录制成功，保存相册成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(baseContext, "保存相册失败", Toast.LENGTH_SHORT).show()
            }
        }

    }

    var currentImgUri: Uri? = null

    override fun onDestroy() {
        super.onDestroy()
        if (viewModel.isRecording()) {
            viewModel.stopRecord()
        }
        glThread?.stopRender()
    }

    var videoPath: String? = null
    private fun waitSurfaceViewReady() {
        Log.i(TAG, "waitSurfaceViewReady")
        videoPath = externalCacheDir!!.absolutePath + "/" + "test.mp4"
        viewModel.initEncode(
            videoPath!!,
            videoEncodeWidth,
            videoEncodeHeight,
            frameRate,
            bitRate,
            iFrameInterval
        )
        binding.cameraTextureView.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {

                    Log.d(TAG, "onSurfaceTextureAvailable size: ${width}x${height}")
                    glThread?.onSurfaceTextureSizeChanged(surface, width, height)
                    glThread?.setInputSurface(
                        viewModel.inputSurface(),
                        frameRate,
                        videoEncodeWidth,
                        videoEncodeHeight
                    )
                    glThread?.start()

                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {

                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    glThread?.onSurfaceTextureDestroyed(surface)
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

                }

            }
    }


    /**
     * 有相机权限后，开启相机
     */
    private fun startCamera() {
        Log.i(TAG, "startCamera")
        //返回当前可以绑定生命周期的 ProcessCameraProvider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            //将相机的生命周期和activity的生命周期绑定，camerax 会自己释放，不用担心了
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // 选择camerax中最接近目标宽高尺寸
            val resolutionSelector =
                CameraxUtils.createResolutionSelector(
                    camera_width,
                    camera_height
                )
            //预览的 capture
            val preview = Preview.Builder()
                // 设置预览尺寸分辨率
                .setResolutionSelector(resolutionSelector)
                .build()

            try {
                // 在重新绑定之前，先解绑所有用例
                cameraProvider.unbindAll()
                Log.i(TAG, "startCamera bind lifecycle")
                // 将用例绑定到相机和当前生命周期
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
                // 获取相机控制
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
                preview.setSurfaceProvider {
                    // 这里使用opengl创建的外部纹理
                    val surfaceTexture: SurfaceTexture = imageSurfaceTexture!!
                    val size: Size = it.resolution
                    Log.d(TAG, "setSurfaceProvider preview size: $size")
                    surfaceTexture.setDefaultBufferSize(size.width, size.height)
                    val surface = Surface(surfaceTexture)
                    // 设置预览的 Surface
                    it.provideSurface(surface, ContextCompat.getMainExecutor(this)) {
                        // 在这里可以处理 surface 释放的回调
                        Log.d(TAG, "Surface released")
                        //surface.release()
                    }
                }
                setAutoFocus()
                setupZoomControls()
            } catch (e: Exception) {
                Log.e(TAG, "用例绑定失败", e)
                Toast.makeText(this, "启动相机失败", Toast.LENGTH_SHORT).show()
            }


        }, ContextCompat.getMainExecutor(this))

    }

    //数字变焦/缩放相关变量
    private var currentZoomRatio = 1.0f
    private var maxZoomRatio = 1.0f
    private var minZoomRatio = 1.0f

    /**
     * 设置缩放变焦
     */

    @SuppressWarnings("all")
    private fun setupZoomControls() {
        // 初始化对焦指示器为隐藏状态
        binding.focusIndicator.visibility = View.GONE
        // 监听缩放状态变化, 相机的当前/最大/最小变焦比例
        cameraInfo.zoomState.observe(this) { zoomState ->
            maxZoomRatio = zoomState.maxZoomRatio
            minZoomRatio = zoomState.minZoomRatio
            currentZoomRatio = zoomState.zoomRatio

            val zoomChange = (currentZoomRatio - minZoomRatio) / (maxZoomRatio - minZoomRatio)
            binding.cameraVerticalProgressBar.setMoveVerticalBias(1 - zoomChange)
            Log.d(
                TAG,
                "Zoom range: $minZoomRatio - $maxZoomRatio, current: $currentZoomRatio"
            )
        }

        // 设置缩放手势监听
        val scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    // 计算出新的缩放值
                    val newZoomRatio = currentZoomRatio * detector.scaleFactor
                    // 限制在范围内
                    val clampedZoomRatio = newZoomRatio.coerceIn(minZoomRatio, maxZoomRatio)

                    //调整缩放情况
                    cameraControl.setZoomRatio(clampedZoomRatio)
                    return true
                }
            })

        binding.cameraTextureView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress && event.action == MotionEvent.ACTION_DOWN) {
                setupClickFocus(view, event)
            }
            return@setOnTouchListener true
        }
        binding.cameraVerticalProgressBar.verticalMoveResultListener =
            object : VerticalProgressBarLayout.VerticalMoveResultListener {
                override fun moveDistance(verticalBias: Float) {
                    // 计算新的缩放值
                    val zoomChange = (maxZoomRatio - minZoomRatio) * (verticalBias)
                    val newZoomRatio = minZoomRatio + zoomChange
                    // 限制在范围内
                    val clampedZoomRatio = newZoomRatio.coerceIn(minZoomRatio, maxZoomRatio)
                    Log.i(TAG, "Zoom change: $zoomChange, new zoom: $clampedZoomRatio")
                    //调整缩放情况
                    cameraControl.setZoomRatio(clampedZoomRatio)
                }

            }

    }


    /**
     * 设置点击对焦
     */
    private fun setupClickFocus(view: View, event: MotionEvent) {
        val textureView = view as TextureView
        // 获取点击位置相对于TextureView的坐标
        val x = event.x
        val y = event.y
        // 创建测光点工厂
        val factory = SurfaceOrientedMeteringPointFactory(
            textureView.width.toFloat(),
            textureView.height.toFloat()
        )
        // 创建测光点
        val point = factory.createPoint(x, y)
        // 创建对焦动作
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(5, TimeUnit.SECONDS) // 设置对焦持续时间
            .build()
        // 执行对焦
        cameraControl.startFocusAndMetering(action)
        // 显示对焦指示器
        showFocusIndicator(x, y)
    }

    /**
     * 显示对焦指示器
     */
    private fun showFocusIndicator(x: Float, y: Float) {
        mainHandler.removeCallbacks(hideFocusIndicatorTask)
        // 显示对焦指示器
        binding.focusIndicator.visibility = View.VISIBLE

        // 计算对焦指示器的位置，使其中心点位于点击位置
        val indicatorWidth = binding.focusIndicator.width.toFloat()
        val indicatorHeight = binding.focusIndicator.height.toFloat()

        // 设置对焦指示器的位置
        binding.focusIndicator.x = x - indicatorWidth / 2
        binding.focusIndicator.y = y - indicatorHeight / 2

        // 创建一个缩放动画
        binding.focusIndicator.scaleX = 1.5f
        binding.focusIndicator.scaleY = 1.5f

        // 动画效果：缩小到原始大小
        binding.focusIndicator.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(500)
            .withEndAction {
                // 动画结束后，延迟一段时间隐藏指示器
                mainHandler.postDelayed(hideFocusIndicatorTask, 500)
            }
            .start()
    }

    private val hideFocusIndicatorTask = Runnable {
        binding.focusIndicator.visibility = View.GONE
    }

    /**
     * 启用连续自动对焦
     */
    private fun setAutoFocus() {
        val autoFocusPoint = SurfaceOrientedMeteringPointFactory(1f, 1f)
            .createPoint(0.5f, 0.5f)
        val action = FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
            .build()
        cameraControl.startFocusAndMetering(action)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                waitSurfaceViewReady()
            } else {
                Toast.makeText(
                    this.application,
                    "你没有授予相机权限。",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // 如果没有权限，关闭应用
            }
        }
    }


    /**
     * 检查所有权限是否已授予
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val TAG: String = MainActivity::class.java.getSimpleName()

        // 当前采用的1080p全高清,视频编码配置
        private val videoEncodeConfig = VideoEncodeConfig.FHD_1080P

        // 相机传感器是横屏,因此与手机预览和视频编码是相反的。
        private val camera_width = videoEncodeConfig.encodeHeight
        private val camera_height = videoEncodeConfig.encodeWidth

        //........ 视频编码配置信息

        // 录制视频的宽高是横屏
        val videoEncodeWidth = videoEncodeConfig.encodeHeight
        val videoEncodeHeight = videoEncodeConfig.encodeWidth
        val frameRate: Int = videoEncodeConfig.frameRate
        private val bitRate = videoEncodeConfig.bitRate
        private val iFrameInterval = videoEncodeConfig.iFrameInterval


        //........ 权限相关
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
            ).toTypedArray()


    }
}
