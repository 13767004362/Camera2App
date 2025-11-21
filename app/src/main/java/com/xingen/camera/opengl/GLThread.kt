package com.xingen.camera.opengl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 一个opengl 线程
 */
class GLThread : Thread() {
    private val TAG = GLThread::class.java.simpleName
    private val lock = Any()
    private var running = true

    private var previewSurfaceTexture: SurfaceTexture? = null

    // 图片纹理的外部纹理对象
    private var imageSurfaceTexture: SurfaceTexture? = null
    private var previewWidth = 0
    private var previewHeight = 0

    //编码器的surface
    private var inputSurface: Surface? = null
    private var encodeWidth: Int = 0
    private var encodeHeight: Int = 0

    // 单一EGL上下文和共享资源
    private var eglContext: EGLContext? = null
    private var eglDisplay: EGLDisplay? = null
    private var eglConfig: EGLConfig? = null

    // 用于预览显示的EGLSurface
    private var previewEglSurface: EGLSurface? = null

    // 用于编码输入的EGLSurface
    private var encodeEglSurface: EGLSurface? = null
    var onFrameAvailableListener: OnFrameAvailableListener? = null

    // 程序id
    var programId: Int = 0

    // 获取顶点数据
    val vertexData = VertexData()

    val vboBuffer = IntArray(1)
    val vaoBuffer = IntArray(1)

    val eboBuffer = IntArray(1)

    //矩阵的索引
    var uTransformMatrix = 0

    var uRotationLocation=0

    val textureBuffer = IntArray(1)

    // 获取SurfaceTexture的变换矩阵
    val transformMatrix = FloatArray(16)
    private var currentRotation = 0f
    // 当前帧数
    var currentFrameCount = 0
    var fps = 0

    //帧开始时间
    var startTimeNs = 0L

    // 是否处于编码中
    @Volatile
    private var isEncoding = false

    var screenshotListener: ScreenshotListener?=null
    @Volatile
    private var isScreenshot = false
    override fun run() {
        initializeEGL()
        initProgram()
        initTextureAndBuffer()
        while (running) {
            synchronized(lock) {
                if (previewEglSurface != null) {
                    // 更新纹理中缓冲
                    imageSurfaceTexture!!.updateTexImage()
                    // 切换到预览的surface中
                    EGL14.eglMakeCurrent(
                        eglDisplay,
                        previewEglSurface,
                        previewEglSurface,
                        eglContext
                    )
                    //设置预览surface的窗口大小
                    GLES30.glViewport(0, 0, previewWidth, previewHeight)
                    drawFrame()
                    EGL14.eglSwapBuffers(eglDisplay, previewEglSurface)
                    // 处理截图
                    handleTakePhoto()

                    if (isEncoding) {

                        // 切换到编码的surface 中
                        EGL14.eglMakeCurrent(
                            eglDisplay,
                            encodeEglSurface,
                            encodeEglSurface,
                            eglContext
                        )
                        //设置编码surface的窗口大小
                        GLES30.glViewport(0, 0, encodeWidth, encodeHeight)
                        drawFrame()
                        handleVideoEncodePTS()
                        // 交换缓冲区
                        val swapResult = EGL14.eglSwapBuffers(eglDisplay, encodeEglSurface)
                        if (!swapResult) {
                            val error = EGL14.eglGetError()
                            Log.e(TAG, "eglSwapBuffers failed for encode, error: $error")
                        }

                        try {
                            // 用于控制帧率刷新。若是fps30，则sleep 33ms
                            val sleepTime = 1000 / fps
                            sleep(sleepTime.toLong())
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        try {
            cleanup()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleVideoEncodePTS() {
        // 设置视频帧的pts时间戳
        // 直播推流,推荐 使用相机的时间戳(imageSurfaceTexture!!.timestamp)
        // val  relativeTimeNS= imageSurfaceTexture!!.timestamp
        // 这里按照固定帧率，累积当前帧来换算，单位纳秒。首帧的pts从0开始
        val relativeTimeNS = (currentFrameCount * 1000000000L / fps)
        // 设置视频帧的pts时间戳
        EGLExt.eglPresentationTimeANDROID(
            eglDisplay,
            encodeEglSurface,
            relativeTimeNS
        )
        currentFrameCount++
    }

    /**
     * 初始化EGL上下文环境
     */
    private fun initializeEGL() {
        // 获取默认显示
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
        // 配置EGL参数
        val configAttrs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttrs, 0, configs, 0, configs.size, numConfigs, 0)
        eglConfig = configs[0]
        // 创建单一EGL上下文 - 使用OpenGL ES 3.0
        val contextAttrs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE
        )

        eglContext =
            EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttrs, 0)
        // 创建预览Surface
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        previewEglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            eglConfig,
            previewSurfaceTexture,
            surfaceAttribs,
            0
        )
        // 创建编码的Surface
        encodeEglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            eglConfig,
            inputSurface!!,
            surfaceAttribs,
            0
        )
        //绑定 预览Surface,不然编译loadShader 会报错
        EGL14.eglMakeCurrent(eglDisplay, previewEglSurface, previewEglSurface, eglContext)
    }

    /**
     * 创建程序和链接着色器
     */
    private fun initProgram() {

        // 简单的顶点着色器
        val vertexShaderCode = "#version 300 es\n" + "layout(location=0) in vec4 aPosition;\n" +
                "layout(location = 1) in vec2 aTexture;\n" +
                "uniform mat4 uTransformMatrix;\n" +
                "uniform float uRotation; \n"+// 添加旋转角度
                "out vec2 vTexture;\n" +
                "void main() {\n" +
                "  vTexture =  (uTransformMatrix * vec4(aTexture, 0, 1)).xy;\n" +
                // 应用旋转
                "  float rad = radians(uRotation);\n"+
                "  mat2 rotationMatrix = mat2(cos(rad), -sin(rad), sin(rad), cos(rad));\n"+
                "  vec2 rotatedPos = rotationMatrix * aPosition.xy;\n"+
                "  gl_Position = vec4(rotatedPos, aPosition.zw);\n" +
                "}"

        // 使用外部OES纹理的片段着色器
        val fragmentShaderCode = "#version 300 es\n" +
                "#extension GL_OES_EGL_image_external_essl3 : require\n" +
                "precision mediump float;\n" +
                " out vec4 fragColor;\n" +
                " in vec2 vTexture;\n" +
                "uniform samplerExternalOES ourTexture;\n" +
                "void main() {\n" +
                "   fragColor = texture(ourTexture, vTexture);\n" +
                "}"

        // 编译着色器
        val vertexShaderId = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShaderId = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // 创建和链接程序
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShaderId)
        GLES30.glAttachShader(programId, fragmentShaderId)
        GLES30.glLinkProgram(programId)
        // 着色器链接成功后，删除着色器对象,不需要使用了。
        GLES30.glDeleteShader(vertexShaderId)
        GLES30.glDeleteShader(fragmentShaderId)
        if (programId != 0) {
            //使用程序
            GLES30.glUseProgram(programId)
        }

    }


    /**
     * 纹理id对应的外部纹理对象的回调器，用于设置到camerax 中preview中
     */
    interface OnFrameAvailableListener {
        fun onFrameAvailable(imageSurfaceTexture: SurfaceTexture)
    }

    private fun loadShader(type: Int, shaderCode: String?): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Shader compilation failed: $error")
        }

        return shader
    }

    private fun initTextureAndBuffer() {
        //获取矩阵：从VERTEX_SHADER 这个顶点着色器代码中获取
        uTransformMatrix = GLES30.glGetUniformLocation(programId, "uTransformMatrix")
        uRotationLocation = GLES30.glGetUniformLocation(programId, "uRotation")
        // 外部纹理，使用GLES11Ext
        GLES30.glGenTextures(1, textureBuffer, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureBuffer[0])
        //纹理环绕
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_REPEAT
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_REPEAT
        )

        //纹理过滤
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_NEAREST
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)


        //创建vbo缓区
        GLES30.glGenBuffers(1, vboBuffer, 0)
        //先创建vao缓冲区
        GLES30.glGenVertexArrays(1, vaoBuffer, 0)
        // 先绑定vao 后绑定vbo
        GLES30.glBindVertexArray(vaoBuffer[0])
        //绑定vbo缓冲区
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboBuffer[0])

        /**
         * 将顶点数据一次性存储到vbo缓冲区中
         * 1.buffer 对象类型： 有GL_ARRAY_BUFFER，GL_ELEMENT_ARRAY_BUFFER，GL_SHADER_STORAGE_BUFFER 等
         * 2.传输数据大小((以字节为单位))： 填写 buffer 大小，由于是 float类型，乘以4
         * 3.实际数据
         * 4.告诉显卡如果管理给定的数据:
         * GL_STATIC_DRAW ：数据不会或几乎不会改变
         * GL_DYNAMIC_DRAW：数据会被改变很多。
         * GL_STREAM_DRAW ：数据每次绘制时都会改变
         */
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertexData.vertexData.capacity() * 4,
            vertexData.vertexData,
            GLES30.GL_STATIC_DRAW
        )

        // 创建ebo 缓冲区
        GLES30.glGenBuffers(1, eboBuffer, 0)
        //注意点：必须先绑定vao 后，才能绑定ebo填充数据，不然会无法显示。
        GLES30.glBindVertexArray(vaoBuffer[0])
        // 绑定ebo,填充数据
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboBuffer[0])
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER, vertexData.indicesData.capacity() * 4,
            vertexData.indicesData, GLES30.GL_STATIC_DRAW
        )


        //接下来，通过vao 解释 顶点数据中位置
        /**
         * GLES30.glVertexAttribPointer(
         *     int index,          // 属性位置（对应shader中的layout location）
         *     int size,           // 每个顶点属性的分量数，这里是2（x, y）
         *     int type,           // 数据类型（GL_FLOAT等）
         *     boolean normalized, // 是否归一化（将整数转为0.0-1.0）
         *     int stride,         // 步长（stride），每个顶点数据占用的字节数（2位置+2纹理坐标，每个float 4字节，4*4=16）。
         *     int offset          // 当前属性起始偏移量
         *);
         */
        GLES30.glVertexAttribPointer(0, vertexData.positionSize, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(0)

        // 解释 顶点数据中纹理坐标, offset :  2个(xy)float * 4字节 = 8字节偏移
        GLES30.glVertexAttribPointer(
            1,
            vertexData.textureSize,
            GLES30.GL_FLOAT,
            false,
            16,
            vertexData.positionSize * vertexData.byteFloat
        )
        GLES30.glEnableVertexAttribArray(1)

        //解绑,vbo 、ebo、vao
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)
        //注意顺序，ebo 要在 vao 之后
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
        // 创建外部纹理对象
        imageSurfaceTexture = SurfaceTexture(textureBuffer[0])
        //有新的帧数据可用时，触发回调。当前自动刷新，不做实现逻辑。若是手动刷新则，需要GLSurfaceView.requestRender()
        imageSurfaceTexture!!.setOnFrameAvailableListener {}
        //通知callBack
        if (onFrameAvailableListener != null) {
            onFrameAvailableListener!!.onFrameAvailable(imageSurfaceTexture!!)
        }
        GLES30.glViewport(0, 0, previewWidth, previewHeight)
        // 设置背景颜色为白色
        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
    }
    fun setRotation(displayRotation: Int) {
        // 根据displayRotation计算需要的旋转角度
        currentRotation= when(displayRotation) {
            Surface.ROTATION_0 -> 0f
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_180 -> 180f
            Surface.ROTATION_270 -> 270f
            else -> 0f
        }
        val isLandscape = displayRotation == Surface.ROTATION_90 || displayRotation == Surface.ROTATION_270
        if (isLandscape){
            //横屏尝试减除180度
            currentRotation -= 180f
        }
    }

    /**
     * 截图处理
     */
    private  fun  handleTakePhoto(){
        if (isScreenshot){
            isScreenshot =false
            val bitmap = OpenglUtils.createBitmapFromGLSurface(0,0, previewWidth,previewHeight)
            screenshotListener?.onScreenshotCaptured(bitmap)
        }
    }

    private fun drawFrame() {
        // 清除和绘制
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureBuffer[0])

        // 获取到camerax 中surfaceTexture的纹理坐标系转换。CameraX的 SurfaceTexture 会自动处理相机预览的旋转和比例变换，通过变换矩阵传递给着色器。
        imageSurfaceTexture?.getTransformMatrix(transformMatrix)
        //将矩阵传递到glsl中，将matrixBuffer的数据，更新到uTransformMatrix 这个索引，更新其值
        GLES30.glUniformMatrix4fv(uTransformMatrix, 1, false, transformMatrix, 0)

        // 用于处理屏幕横竖屏带来的旋转问题：添加旋转角度的uniform变量
        GLES30.glUniform1f(uRotationLocation, currentRotation)

        //绘制时，绑定vao
        GLES30.glBindVertexArray(vaoBuffer[0])
        /**
         * 使用ebo 后，不再使用 glDrawArrays， 替换成了 glDrawElements ，表示我们要从索引缓存中渲染三角形
         * 1.绘制类型：三角形
         * 2:顶点数据的个数，2个三角形=3*2
         * 3.索引的类型，这里是GL_UNSIGNED_INT
         * 4.偏移量
         */
        GLES30.glDrawElements(
            GLES30.GL_TRIANGLES,
            vertexData.indices.size,
            GLES30.GL_UNSIGNED_INT,
            0
        )
        // 绘制完成后，解绑
        GLES30.glBindVertexArray(0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        this.previewSurfaceTexture = surfaceTexture
        previewWidth = width
        previewHeight = height
    }

    /**
     * 设置编码器的surface
     */
    fun setInputSurface(surface: Surface, fps: Int, width: Int, height: Int) {
        this.encodeWidth = width
        this.encodeHeight = height
        this.inputSurface = surface
        this.fps = fps
    }

    fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture?) {
        if (imageSurfaceTexture != null) {
            imageSurfaceTexture!!.release()
            imageSurfaceTexture = null
        }
    }

    /**
     * 设置开启或者停止编码模式
     */
    fun setEncode(encoding: Boolean) {
        this.isEncoding = encoding
        if (encoding)
            startTimeNs = 0L
    }

    /**
     * 停止渲染
     */
    fun stopRender() {
        running = false
        interrupt()
    }


    /**
     * 截图功能
     */
    fun takePhoto(){
        isScreenshot = true
    }

    // 拍照回调
    interface ScreenshotListener {
        fun onScreenshotCaptured(bitmap: Bitmap)
    }
    private fun cleanup() {
        if (imageSurfaceTexture != null) {
            imageSurfaceTexture!!.release()
            imageSurfaceTexture = null
        }
        if (previewEglSurface != null) {
            EGL14.eglDestroySurface(eglDisplay, previewEglSurface)
            previewEglSurface = null
        }
        if (encodeEglSurface != null) {
            EGL14.eglDestroySurface(eglDisplay, encodeEglSurface)
            encodeEglSurface = null
        }
        if (eglContext != null) {
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            eglContext = null
        }
        if (eglDisplay != null) {
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = null
        }
        // 释放纹理
        if (textureBuffer.isNotEmpty()) {
            GLES30.glDeleteBuffers(1, textureBuffer, 0)
        }
        // 释放各种缓冲区
        if (vaoBuffer.isNotEmpty()) {
            GLES30.glGenVertexArrays(1, vaoBuffer, 0)
        }
        if (vboBuffer.isNotEmpty()) {
            GLES30.glDeleteBuffers(1, vboBuffer, 0)
        }
        if (eboBuffer.isNotEmpty()) {
            GLES30.glDeleteBuffers(1, eboBuffer, 0)
        }
        //释放程序
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
        }
    }

    /**
     * 用于绘制点的顶点数据
     */
    inner class VertexData {
        // 顶点数据：位置(x,y)  + 纹理坐标(s,t)
        val vertex = floatArrayOf(
            // 矩形4个顶点（2个三角形构成）,避免存储相同的顶点数据

            1.0f, 1.0f, 1.0f, 1.0f,   // 右上角
            1.0f, -1.0f, 1.0f, 0.0f,  // 右下角
            -1.0f, -1.0f, 0.0f, 0.0f,  // 左下角
            -1.0f, 1.0f, 0.0f, 1.0f  // 左上角
        )

        // ebo 索引数据
        val indices = intArrayOf(
            // 注意索引从0开始!
            // 此例的索引(0,1,2,3)就是顶点数组vertices的下标，
            // 这样可以由下标代表顶点组合成矩形

            0, 1, 3, // 第一个三角形
            1, 2, 3  // 第二个三角形
        )

        // 分量的字节数：每个分量是float类型，4个字节
        val byteFloat = 4
        val byteInt = 4

        // 每个顶点数据中分量个数：当前x,y坐标是2个分量
        val positionSize = 2


        // 每个顶点数据中分量个数：当前st是2个分量
        val textureSize = 2
        val indicesData = ByteBuffer
            // 分配顶点坐标分量个数 * Int占的Byte位数
            .allocateDirect(indices.size * byteInt)
            .order(ByteOrder.nativeOrder())
            // Byte类型转Int类型
            .asIntBuffer()
            .put(indices)
            //将缓冲区的指针指到头部，保证数据从头开始
            .position(0)

        /**
         * OpenGL ES需要使用 DirectByteBuffer 对象,这种对象可以直接映射到GPU的内存中,而普通数组则无法直接映射
         */
        val vertexData = ByteBuffer
            // 分配顶点坐标分量个数 * Float占的Byte位数
            .allocateDirect(vertex.size * byteFloat)
            .order(ByteOrder.nativeOrder())
            // Byte类型转Float类型
            .asFloatBuffer()
            .put(vertex)
            //将缓冲区的指针指到头部，保证数据从头开始
            .position(0)

    }
}