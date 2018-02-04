# Camera2App
采用android5.0中Camera2 api+MVP模式开发的一个相机程序。

-----
包含以下功能：

- 拍照：自动调焦，闪关灯，手动调焦

- 录像：一次性录像，断续录像。

----

采用的技术点：

- Camera2 API：相机API

- soviewer.jar: 用于将多个视频合成一个视频

- RxJava和RxAndroid: 异步线程和主线程通讯

- Glide : 异步加载图片和视频

待开发模块：近远景调焦拍照。
----
备注：

虽然andorid 5.0 出现了camera 2 API特性，但各大android手机系统厂商对camera 2 的支持力度不一致。
因此，实际开发中还应该通过判断运行手机对camera2 支持力度来决定是否需要用回camera来开发相机。
