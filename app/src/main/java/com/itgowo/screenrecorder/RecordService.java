package com.itgowo.screenrecorder;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
/**
 * @author lujianchao
 * 2018-07-26 18:27:13
 */

public class RecordService extends Service {
    private static final int RECORD_REQUEST_CODE = 1011;
    private static final int STORAGE_REQUEST_CODE = 1012;
    private static final int AUDIO_REQUEST_CODE = 1013;
    public static final String OUT_FILE = "out_file";


    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private String outFile;

    private boolean running;
    private int width = 720;
    private int height = 1080;
    private int dpi;

    private onStatusListener listener;

    @Override
    public IBinder onBind(Intent intent) {
        outFile = intent.getStringExtra(OUT_FILE);
        return new RecordBinder();
    }

    public onStatusListener getStatusListener() {
        return listener;
    }

    public void setStatusListener(onStatusListener listener) {
        this.listener = listener;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        HandlerThread serviceThread = new HandlerThread("service_thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
        mediaRecorder = new MediaRecorder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void requestCreateScreenCaptureIntent(Activity activity) {
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
    }

    public void requestPermission(AppCompatActivity context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    public boolean startRecord() {
        if (mediaProjection == null || running) {
            return false;
        }
        if (!hasPermissions()) {
            listener.onPermissionsDenied(this);
            return false;
        }
        initRecorder();
        createVirtualDisplay();
        mediaRecorder.start();
        running = true;
        return true;
    }

    public boolean hasPermissions() {
        PackageManager pm = getApplication().getPackageManager();
        String packageName = getApplication().getPackageName();
        int granted = pm.checkPermission(RECORD_AUDIO, packageName) | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;
        mediaRecorder.stop();
        mediaRecorder.reset();
        virtualDisplay.release();
        mediaProjection.stop();
        listener.onStop(outFile);
        return true;
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    private void initRecorder() {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(outFile);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(30);
        try {
            mediaRecorder.prepare();
            listener.onStart(this);
        } catch (IOException e) {
            listener.onError(e);
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            listener.onPrepare(this);
        }
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }

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
}