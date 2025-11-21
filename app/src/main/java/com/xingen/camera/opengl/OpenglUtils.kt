package com.xingen.camera.opengl

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * Created by HeXinGen  on 2025/7/8
 * Description:
 *
 * opengl es 的工具类
 *
 */
object OpenglUtils {
    private const val TAG = "OpenglUtils"

    /**
     * 创建程序，加载着色器进行链接，组装成一个OpenGL程序
     */
    fun makeProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        // 创建顶点着色器
        val vertexShaderId = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        if (vertexShaderId == 0) {
            printLog("linkProgram: vertex shader create failed")
            return 0
        }
        // 创建片段着色器
        val fragmentShaderId = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)
        if (fragmentShaderId == 0) {
            printLog( "linkProgram: fragment shader create failed")
            return 0
        }
        //将顶点着色器、片段着色器进行链接，组装成一个OpenGL程序
        val programId = linkProgram(vertexShaderId, fragmentShaderId)
        // 着色器链接成功后，删除着色器对象,不需要使用了。
        GLES30.glDeleteShader(fragmentShaderId)
        GLES30.glDeleteShader(vertexShaderId)
        if (programId != 0) {
            //使用程序
            GLES30.glUseProgram(programId)
        }

        return programId
    }

    /**
     * 链接着色器程序
     */
    fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        // 创建程序对象
        val programId = GLES30.glCreateProgram()
        if (programId == 0) {
            printLog( "linkProgram: create program failed")
            return 0
        }
        // 将着色器添加到程序中
        GLES30.glAttachShader(programId, vertexShaderId)
        GLES30.glAttachShader(programId, fragmentShaderId)
        // 链接程序：将两个着色器关联到opengl 对象
        GLES30.glLinkProgram(programId)

        // 检查链接状态
        val status = intArrayOf(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            printLog("linkProgram: link program failed ${GLES30.glGetProgramInfoLog(programId)}")
            GLES30.glDeleteProgram(programId)
            return 0
        }
        return programId
    }


    /**
     * 创建且 加载着色器
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        // 创建着色器对象
        val shaderId = GLES30.glCreateShader(type)
        if (shaderId == 0) {
            printLog( "loadShader: create shader failed")
            return 0
        }
        // 编译着色器代码
        GLES30.glShaderSource(shaderId, shaderCode)
        //编译对象
        GLES30.glCompileShader(shaderId)
        // 检查编译状态
        val status = intArrayOf(1)

        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            printLog("loadShader: compile shader failed ${GLES30.glGetShaderInfoLog(shaderId)}}")
            GLES30.glDeleteShader(shaderId)
            return 0
        }
        return shaderId
    }

    /**
     * 创建纹理对象，绑定bitmap数据
     */
    fun  loadTexture(bitmap: Bitmap): TextureData?{
         val textureBuffer = IntArray(1)
         // 创建纹理对象
          GLES30.glGenTextures(1,textureBuffer,0)
         if (textureBuffer[0]==0){
             printLog(" loadTexture 创建纹理对象失败")
             return  null
         }
        // 绑定纹理对象
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textureBuffer[0])
        //纹理环绕
       // GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_REPEAT)
      //  GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_REPEAT)

        //纹理过滤
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR)

        //绑定数据： 将位图数据上传到GPU
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,bitmap,0)
        //生成 mip 位图 多级渐远纹理
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)

        //释放纹理对象
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0)
        return TextureData(textureId = textureBuffer[0], bitmapWidth = bitmap.width, bitmapHeight = bitmap.height)
    }


    /**
     * 执行完gl 操作后检测是否存在错误
     */
    fun  checkGLError(content: String){
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
             printLog( "$content   ,GL Error: $error")
        }
    }

    private  fun  printLog(content: String){
        Log.e(TAG, content)

    }


    /**
     * 截屏：
     * 从帧数据中读取，生成一张图片
     */
    fun createBitmapFromGLSurface(x: Int = 0, y: Int = 0, w: Int, h: Int): Bitmap {
        val pixelCount = w * h
        val rgbaBuffer = ByteBuffer.allocateDirect(pixelCount * 4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(x, y, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, rgbaBuffer)

        val bitmapBuffer = IntArray(pixelCount)
        val rgbaArray = ByteArray(pixelCount * 4)
        rgbaBuffer.get(rgbaArray)

        // 翻转并转换通道
        for (i in 0 until h) {
            for (j in 0 until w) {
                val index = (i * w + j) * 4
                val r = rgbaArray[index].toInt() and 0xFF
                val g = rgbaArray[index + 1].toInt() and 0xFF
                val b = rgbaArray[index + 2].toInt() and 0xFF
                val a = rgbaArray[index + 3].toInt() and 0xFF
                // ARGB_8888 格式
                bitmapBuffer[(h - 1 - i) * w + j] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(bitmapBuffer, w, h, Bitmap.Config.ARGB_8888)
    }
}


/**
 * 纹理信息对象
 */
data  class  TextureData(var textureId: Int=-1, var bitmapWidth: Int=0,var bitmapHeight: Int =0)

/**
 * 存储fbo的信息
 *
 */
data class FBOData(var fboId: Int,var textureId: Int,var width: Int,var height: Int,var rboId: Int=0)