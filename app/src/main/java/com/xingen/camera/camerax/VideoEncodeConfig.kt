package com.xingen.camera.camerax

/**
 * Created by HeXinGen  on 2025/11/17
 * Description:
 * 视频编码的参数配置，借鉴webrtc。
 *
 */
enum class VideoEncodeConfig(
    // 编码宽度
    val encodeWidth: Int,
    // 编码高度
    val encodeHeight: Int,
    // 帧率
    val frameRate: Int,
    //码率
    val bitRate: Int,
    //关键帧间隔，单位秒。一般是pts的两倍。
    val iFrameInterval: Int
) {
    //高清,应用场景：移动网络/普通宽带
    HD_720P(720, 1280, 30, 2500_000, 2),

    //全高清,应用场景：wifi/良好宽带
    FHD_1080P(1080, 1920, 30, 4500_000, 2),

    // 超高清,应用场景：高速宽带、局域网
    UHD_4K_2160P(2160, 3840, 60, 15000_000, 2)
}