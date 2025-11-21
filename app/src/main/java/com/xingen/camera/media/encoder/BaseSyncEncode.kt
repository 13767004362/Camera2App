package com.xingen.camera.media.encoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.xingen.camera.media.muxer.MediaMuxerHandler

import java.nio.ByteBuffer

/**
 * Created by HeXinGen  on 2025/11/14
 * Description:.
 */
abstract class BaseSyncEncode(var isAudio: Boolean) {
    private val TAG = "BaseSyncEncode"

    protected var mediaCodec: MediaCodec? = null
    protected var mediaFormat: MediaFormat? = null
    protected var encodePacketCallBack: EncodePacketCallBack? = null

    @Volatile
    protected var isRunning = false
    private var thread: Thread? = null

    // pts 时间
    protected var presentationTimeUs: Long = 0

    protected var lastPresentationTimeUs: Long = 0


    fun init(mediaMuxerHandler: MediaMuxerHandler) {

        initMediaCodec()
        if (mediaFormat != null) {
            // 添加轨道
            mediaMuxerHandler.addTrack(mediaFormat!!, isAudio)
        }

    }


    /**
     * 开始解码
     */
    fun start(encodePacketCallBack: EncodePacketCallBack) {
        if (mediaCodec != null) {
            this.encodePacketCallBack = encodePacketCallBack
            isRunning = true
            mediaCodec!!.start()
            // 启动编码线程
            thread = Thread {
                loopEncode()
            }
            thread?.start()
        }
    }

    fun stop() {
        if (isRunning) {
            handleEndOfInputStream()
            isRunning = false
        }
        thread?.join()
        encodePacketCallBack = null
    }

    /**
     * 循环编码
     */

    private fun loopEncode() {
        mediaCodec?.let {
            val bufferInfo = MediaCodec.BufferInfo()
            while (isRunning) {
                try {
                    // 处理编码后的输出
                    val outputBufferId = it.dequeueOutputBuffer(bufferInfo, 1000)
                    if (outputBufferId >= 0) {
                        val outputBuffer = it.getOutputBuffer(outputBufferId)
                        // 检查是否为配置数据（CSD）
                        val isConfigData = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0

                        if (outputBuffer != null && bufferInfo.size > 0) {
                            // 处理编码后的aac数据或者264数据 ,需要检查 pts 时间戳单调递增
                            checkPTS(bufferInfo)
                            checkVideoKeyFrame(bufferInfo)
                            encodePacketCallBack?.onEncodePacket(outputBuffer, bufferInfo, isAudio)
                        }else if (isConfigData && outputBuffer != null){
                            // 对于配置数据，即使size为0也要写入，这是MP4文件必需的
                            //- AAC音频需要AudioSpecificConfig
                            //- H.264视频需要SPS/PPS数据
                            Log.d(TAG, "Writing codec config data for ${if(isAudio) "audio" else "video"}")
                            encodePacketCallBack?.onEncodePacket(outputBuffer, bufferInfo, isAudio)
                        }
                        it.releaseOutputBuffer(outputBufferId, false)
                        // 检查是否到达流结束
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.i(TAG, "encoder EOS")
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // 释放资源
            it.stop()
            it.release()
        }
    }

   protected fun checkVideoKeyFrame(bufferInfo: MediaCodec.BufferInfo){
       if (!isAudio){
           // 添加关键帧检测
           val isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
           if (isKeyFrame) {
               Log.d(TAG, "Video frame - KeyFrame  pts: ${bufferInfo.presentationTimeUs}")
           }
       }
   }

    /**
     * 编码后的数据包
     */
    interface EncodePacketCallBack {
        fun onEncodePacket(
            byteBuffer: ByteBuffer,
            bufferInfo: MediaCodec.BufferInfo, isAudio: Boolean
        )
    }


    /**
     * 检查帧的pts
     */
    protected abstract fun checkPTS(bufferInfo: MediaCodec.BufferInfo)

    protected abstract fun initMediaCodec()

    // 必须发送流结束标识
    protected abstract  fun  handleEndOfInputStream()


}