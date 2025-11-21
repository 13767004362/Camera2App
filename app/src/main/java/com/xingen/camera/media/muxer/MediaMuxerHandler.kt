package com.xingen.camera.media.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue


/**
 * Created by HeXinGen  on 2025/11/13
 * Description:
 *
 * 将编码后的h264/avc,aac音频压缩数据,通过mediaMuxer合成mp4文件。
 *
 * 注意点：mediaMuxer 必须至少添加一个轨道,才能启动。
 *
 */
class MediaMuxerHandler {
    val TAG = "MediaMuxerHandler"

    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1 // 视频轨道索引
    private var audioTrackIndex = -1 // 音频轨道索引

    @Volatile
    private var isRunning = false // 混合器是否启动
    private var outputPath = "" // 输出文件路径
    private var workThread: Thread? = null
    private val muxerDataQueue = ArrayBlockingQueue<MuxerData>(100)

    // 用于存储 音视频编码后数据
    data class MuxerData(
        val trackIndex: Int,
        val byteBuffer: ByteBuffer,
        val bufferInfo: MediaCodec.BufferInfo
    )

    /**
     * 初始化
     */
    fun init(outputPath: String) {
        this.outputPath = outputPath
        try {
            //封装格式：mp4
            val format = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            mediaMuxer = MediaMuxer(outputPath, format)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 添加轨道
     */
    @Synchronized
    fun addTrack(mediaFormat: MediaFormat, isAudio: Boolean): Int {
        if (mediaMuxer == null) {
            Log.i(TAG, " addTrack mediaMuxer is null ")
            return -1
        }

        val trackIndex = mediaMuxer!!.addTrack(mediaFormat)
        if (isAudio) {
            audioTrackIndex = trackIndex
        } else {
            videoTrackIndex = trackIndex
        }
        // mediaMuxer必须添加至少一个轨道后才能start
        if (videoTrackIndex >= 0 && audioTrackIndex >= 0 && !isRunning) {
            // 开始混合器
            isRunning = true
            workThread = Thread { processQueue() }
            workThread?.start()
            Log.i(TAG, " start mediaMuxer ")
        }
        return trackIndex
    }

    /**
     * 写入mediacodec 编码后数据
     */

    fun writeSampleData(
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        isAudio: Boolean
    ) {
        if (isRunning) {
            muxerDataQueue.offer(
                MuxerData(
                    if (isAudio) audioTrackIndex else videoTrackIndex,
                    byteBuffer,
                    bufferInfo
                )
            )
        }
    }

    private fun processQueue() {
        mediaMuxer!!.start()
        Log.i(TAG, "MediaMuxer started successfully")
        while (isRunning) {
            var muxerData: MuxerData? = null
            try {
                muxerData = muxerDataQueue.take()
            } catch (e: Exception) {
                if (e is InterruptedException) {
                    break
                }
                e.printStackTrace()
            }
            if (muxerData != null) {
                // 记录写入的数据信息，用于调试
                val isConfigData = muxerData.bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                val isKeyFrame = muxerData.bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                val isEndOfStream = muxerData.bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                if (isConfigData) {
                    Log.d(TAG, "Writing codec config data for track ${muxerData.trackIndex}, size: ${muxerData.bufferInfo.size}")
                } else if (isKeyFrame) {
                    Log.d(TAG, "Writing key frame for track ${muxerData.trackIndex}, pts: ${muxerData.bufferInfo.presentationTimeUs}")
                }
                mediaMuxer?.writeSampleData(
                    muxerData.trackIndex,
                    muxerData.byteBuffer,
                    muxerData.bufferInfo
                )
                if (isEndOfStream) {
                    Log.i(TAG, "Received end of stream for track ${muxerData.trackIndex}")
                }
            }
        }
        try {
            mediaMuxer!!.stop()
            mediaMuxer!!.release()
        }catch (e: Exception){
            Log.e(TAG, "Error stopping/releasing MediaMuxer: ${e.message}")
            e.printStackTrace()
        }
        Log.i(TAG, "MediaMuxer released successfully")
        audioTrackIndex = -1
        videoTrackIndex = -1
    }

    fun stop() {
        isRunning = false
        workThread?.interrupt()
        workThread?.join()
        workThread=null
    }


}