package com.itgowo.screenrecorder;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends AppCompatActivity {


    private RecordService recordService;
    private Button startBtn;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        startBtn = (Button) findViewById(R.id.start_record);
        startBtn.setEnabled(false);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordService.isRunning()) {
                    recordService.stopRecord();
                    startBtn.setText(R.string.start_record);
                } else {
                    recordService.requestCreateScreenCaptureIntent(MainActivity.this);
                }
            }
        });


        Intent intent = new Intent(this, RecordService.class);
        file = new File(getSaveDirectory(), System.currentTimeMillis() + ".mp4");
        intent.putExtra(RecordService.OUT_FILE, file.getAbsolutePath());
        bindService(intent, connection, BIND_AUTO_CREATE);
    }


    public File getSaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File rootDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "test");
            if (!rootDir.exists()) {
                if (!rootDir.mkdirs()) {
                    return null;
                }
            }
            return rootDir;
        } else {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        recordService.onActivityResult(requestCode, resultCode, data);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();
            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            recordService.setStatusListener(new RecordService.onStatusListener() {

                @Override
                public void onPrepare(RecordService service) {
                    showToast("onPrepare");
                    service.startRecord();
                }

                @Override
                public void onStart(RecordService service) {
                    showToast("onStart");
                    startBtn.setText(R.string.stop_record);
                }

                @Override
                public void onStop(String outFile) {
                    showToast(outFile);
                }

                @Override
                public void onPermissionsDenied(RecordService service) {
                    service.requestPermission(MainActivity.this);
                    showToast("onPermissionsDenied");
                }

                @Override
                public void onError(Throwable throwable) {
                    showToast(throwable.getMessage());
                    throwable.printStackTrace();
                }
            });
            startBtn.setEnabled(true);
            startBtn.setText(recordService.isRunning() ? R.string.stop_record : R.string.start_record);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
