# ScreenRecorder
Android Lollipop (5.0 ,api=21) 及更高版本屏幕录制实现

http://itgowo.com

QQ:1264957104

Email:lujianchao@itgowo.com

## 基本原理
#### 在 Android5.0，Google终于开放了视频录制的接口（屏幕采集的接口），主要实现类MediaProjection 和 MediaProjectionManager，存储视频使用MediaRecord（简单）。或者更复杂一点用DisplayManager，VirtualDisplay，MediaCodec，MediaFormat，MediaMuxer，MediaProjection打套组合拳，我吐血了，虽然成功了，但是视频播放勉强过关，问题出在哪里我没找到。

#### 使用Service而没采用工具类方式，其他人也写了好多类似的，写个不一样的。

## 步骤

#### 1.申请权限

        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
        <uses-permission android:name="android.permission.RECORD_AUDIO"/>

针对动态权限Service封装了检查hasPermissions()和申请方法requestPermission(),提供录制状态回调，如果内部检查没有足够权限回调
        
         /**
         * 权限不足，可以用 service.requestPermission(MainActivity.this);发起请求
         * @param service
         */
        void onPermissionsDenied(RecordService service);


#### 2 获取 MediaProjectionManager

    MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

#### 3.请求屏幕录像
    Intent captureIntent= projectionManager.createScreenCaptureIntent(); 
    startActivityForResult(captureIntent, REQUEST_CODE);

#### 4.获取 MediaProjection
通过 onActivityResult 返回结果获取 MediaProjection。
内部封装了，onActivityResult(),在activity中调用一下。

     @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        recordService.onActivityResult(requestCode, resultCode, data);
    }

内部封装方法

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            listener.onPrepare(this);
        }
    }

#### 5.创建虚拟屏幕
这一步就是通过 MediaProject 录制屏幕的关键所在，VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR 参数是指创建屏幕镜像，所以我们实际录制内容的是屏幕镜像，但内容和实际屏幕是一样的，并且这里我们把 VirtualDisplay 的渲染目标 Surface 设置为 MediaRecorder 的 getSurface，后面我就可以通过 MediaRecorder 将屏幕内容录制下来，并且存成 video 文件

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen",width,height,dpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mediaRecorder.getSurface(),null, null);
    }

#### 6 录制屏幕数据
这里利用 MediaRecord 将屏幕内容保存下来，也可以用其它方式保存，例如：ImageReader(繁琐)
    
    private void initRecorder() {
        File file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".mp4");
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(file.getAbsolutePath());
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(30);
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }


## 状态回调
算是特色吧

    interface onStatusListener {
        /**
         * 获得录屏许可调用此方法，可以执行service.startRecord();
         * @param service
         */
        void onPrepare(RecordService service);
        /**
         * 开始录屏，可以在此回调中修改文案
         * @param service
         */
        void onStart(RecordService service);
        /**
         * 录制停止
         */
        void onStop(String outFile);
        /**
         * 权限不足，可以用 service.requestPermission(MainActivity.this);发起请求
         * @param service
         */
        void onPermissionsDenied(RecordService service);
        void onError(Throwable throwable);
    }














